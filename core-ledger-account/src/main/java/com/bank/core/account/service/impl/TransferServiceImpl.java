package com.bank.core.account.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bank.core.account.entity.Account;
import com.bank.core.account.entity.TransferOrder;
import com.bank.core.account.mapper.AccountMapper;
import com.bank.core.account.mapper.TransferOrderMapper;
import com.bank.core.account.service.TransferService;
import com.bank.core.api.dto.TransferDTO;
import com.bank.core.api.dto.TransferQueryDTO;
import com.bank.core.api.dto.TransactionCreateDTO;
import com.bank.core.api.dto.TransactionEntryDTO;
import com.bank.core.account.config.PrometheusConfig;
import com.bank.core.api.event.AccountEvent;
import com.bank.core.api.vo.TransferOrderVO;
import com.bank.core.common.constants.CommonConstants;
import com.bank.core.common.enums.*;
import com.bank.core.common.exception.BusinessException;
import com.bank.core.common.utils.AmountUtil;
import com.bank.core.common.utils.DistributedLockUtil;
import com.bank.core.common.utils.IdempotentUtil;
import com.bank.core.common.utils.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 转账服务实现类
 * 处理账户间转账业务，保证扣款方与收款方的原子操作
 *
 * 核心业务逻辑：
 * 1. 账户间转账：内部账户A → 内部账户B
 * 2. 保证原子性：使用Seata全局事务，要么全部成功，要么全部失败
 * 3. 支持备注功能
 *
 * 技术要点：
 * - 全链路幂等性校验（Redis + DB + 分布式锁）
 * - Seata全局事务保证跨账户操作的原子性
 * - 借贷记账法：借记收款人账户，贷记付款人账户
 * - 分布式锁防止并发操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final TransferOrderMapper transferOrderMapper;
    private final AccountMapper accountMapper;
    private final TransactionServiceImpl transactionService;
    private final RedissonClient redissonClient;
    private final RocketMQTemplate rocketMQTemplate;
    private final PrometheusConfig prometheusConfig;
    private final com.bank.core.account.service.DistributedTransactionService distributedTransactionService;
    private final com.bank.core.account.service.MessageProducerService messageProducerService;

    /**
     * 账户间转账
     * 资金流向：内部账户A（扣款方） → 内部账户B（收款方）
     *
     * 业务流程：
     * 1. 幂等性校验（Redis + DB + 分布式锁三层校验）
     * 2. 参数校验（币种、金额、账户状态、余额充足）
     * 3. 校验转账双方不是同一账户、币种一致
     * 4. 分布式锁保证并发安全
     * 5. 创建转账订单（状态：待处理）
     * 6. 创建会计交易（借记收款人账户，贷记付款人账户）
     * 7. 更新订单状态为成功
     * 8. 发送转账事件到MQ
     * 9. 清除账户缓存
     *
     * 注意：使用Seata全局事务保证跨账户操作的原子性
     *
     * @param dto 转账请求DTO
     * @return 转账订单VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransferOrderVO transfer(TransferDTO dto) {
        log.info("开始账户间转账, businessNo: {}, fromAccountId: {}, toAccountId: {}, amount: {}, useTcc: {}",
                dto.getBusinessNo(), dto.getFromAccountId(), dto.getToAccountId(), dto.getAmount(),
                dto.getDistributedTxType());

        if (dto.getDistributedTxType() != null
                && DistributedTransactionTypeEnum.TCC.getCode().equals(dto.getDistributedTxType())) {
            log.info("使用TCC模式执行行内转账, businessNo: {}", dto.getBusinessNo());
            return transferWithTcc(dto);
        }

        log.info("使用传统AT模式执行行内转账, businessNo: {}", dto.getBusinessNo());
        return prometheusConfig.recordTransferLatency(() -> {
            try {
                IdempotentUtil.checkIdempotent(dto.getRequestId());
                TransferOrderVO result = doTransfer(dto);
                prometheusConfig.recordTransactionSuccess();
                prometheusConfig.recordDistributedTransactionSuccess();
                return result;
            } catch (Exception e) {
                prometheusConfig.recordTransactionFailure();
                prometheusConfig.recordDistributedTransactionFailure();
                if (e instanceof BusinessException) {
                    BusinessException be = (BusinessException) e;
                    if (ResultCodeEnum.CONCURRENT_UPDATE_FAILED.getCode().equals(be.getCode())) {
                        prometheusConfig.recordHotAccountConflict();
                    }
                    if (ResultCodeEnum.ACCOUNT_FROZEN.getCode().equals(be.getCode())) {
                        prometheusConfig.incrementFrozenAccountExceptions();
                    }
                }
                throw e;
            }
        });
    }

    /**
     * 行内转账 - TCC模式
     * 通过分布式事务服务调用TCC接口
     */
    private TransferOrderVO transferWithTcc(TransferDTO dto) {
        log.info("行内转账-TCC模式, businessNo: {}", dto.getBusinessNo());

        String xid = distributedTransactionService.transferWithTcc(dto);

        TransferOrder order = transferOrderMapper.selectByBusinessNo(dto.getBusinessNo());
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.TRANSFER_ORDER_NOT_EXIST, "TCC转账订单不存在");
        }

        TransferOrderVO vo = convertToVO(order);
        vo.setRemark("TCC转账处理完成，全局事务ID: " + xid);

        log.info("行内转账-TCC模式完成, businessNo: {}, xid: {}, transferId: {}",
                dto.getBusinessNo(), xid, order.getTransferId());
        return vo;
    }

    private TransferOrderVO doTransfer(TransferDTO dto) {

        validateTransferParam(dto);

        String idempotentKey = CommonConstants.TRANSFER_IDEMPOTENT_PREFIX + dto.getBusinessNo();
        RBucket<String> idempotentBucket = redissonClient.getBucket(idempotentKey);
        String cachedTransferId = idempotentBucket.get();
        if (cachedTransferId != null) {
            log.warn("幂等命中, businessNo: {}, transferId: {}", dto.getBusinessNo(), cachedTransferId);
            TransferOrder existOrder = transferOrderMapper.selectByTransferId(cachedTransferId);
            if (existOrder != null) {
                return convertToVO(existOrder);
            }
        }

        TransferOrder existOrder = transferOrderMapper.selectByBusinessNo(dto.getBusinessNo());
        if (existOrder != null) {
            log.warn("业务单号已存在, businessNo: {}", dto.getBusinessNo());
            idempotentBucket.set(existOrder.getTransferId(), 24, TimeUnit.HOURS);
            return convertToVO(existOrder);
        }

        String lockKey = CommonConstants.TRANSFER_LOCK_PREFIX + dto.getBusinessNo();
        return DistributedLockUtil.executeWithLock(lockKey, () -> {
            TransferOrder existAgain = transferOrderMapper.selectByBusinessNo(dto.getBusinessNo());
            if (existAgain != null) {
                log.warn("分布式锁内二次检查发现业务单号已存在, businessNo: {}", dto.getBusinessNo());
                idempotentBucket.set(existAgain.getTransferId(), 24, TimeUnit.HOURS);
                return convertToVO(existAgain);
            }

            Account fromAccount = validateAccount(dto.getFromAccountId(), dto.getCurrency());
            Account toAccount = validateAccount(dto.getToAccountId(), dto.getCurrency());

            if (dto.getFromAccountId().equals(dto.getToAccountId())) {
                throw new BusinessException(ResultCodeEnum.TRANSFER_SAME_ACCOUNT);
            }

            if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
                throw new BusinessException(ResultCodeEnum.TRANSFER_CURRENCY_MISMATCH);
            }

            validateSufficientBalance(fromAccount, dto.getAmount());

            String transferId = SnowflakeIdGenerator.nextIdStr();
            String transferNo = SnowflakeIdGenerator.generateTransferNo();

            TransferOrder order = buildTransferOrder(dto, transferId, transferNo, fromAccount, toAccount);
            transferOrderMapper.insert(order);

            try {
                String transactionId = createTransferTransaction(dto, fromAccount, toAccount, transferId);

                order.setStatus(PaymentStatusEnum.SUCCESS.getCode());
                order.setTransactionId(transactionId);
                order.setTransferTime(LocalDateTime.now());
                order.setUpdateTime(LocalDateTime.now());
                transferOrderMapper.updateById(order);

                TransferOrderVO vo = convertToVO(order);

                idempotentBucket.set(transferId, 24, TimeUnit.HOURS);

                sendTransferEvent(vo);

                deleteAccountCache(dto.getFromAccountId());
                deleteAccountCache(dto.getToAccountId());

                log.info("账户间转账成功, transferId: {}, transferNo: {}", transferId, transferNo);
                return vo;
            } catch (Exception e) {
                log.error("账户间转账失败, transferId: {}", transferId, e);
                order.setStatus(PaymentStatusEnum.FAILED.getCode());
                order.setUpdateTime(LocalDateTime.now());
                transferOrderMapper.updateById(order);
                throw e;
            }
        });
    }

    /**
     * 根据转账ID查询转账订单
     * 先查缓存，缓存不存在再查数据库，查询结果写入缓存
     * @param transferId 转账订单ID
     * @return 转账订单VO
     */
    @Override
    public TransferOrderVO getTransferOrder(String transferId) {
        log.debug("查询转账订单, transferId: {}", transferId);

        String cacheKey = CommonConstants.TRANSFER_CACHE_PREFIX + transferId;
        RBucket<String> cacheBucket = redissonClient.getBucket(cacheKey);
        String cacheValue = cacheBucket.get();
        if (cacheValue != null) {
            TransferOrderVO vo = JSON.parseObject(cacheValue, TransferOrderVO.class);
            log.debug("从缓存获取转账订单成功, transferId: {}", transferId);
            return vo;
        }

        TransferOrder order = transferOrderMapper.selectByTransferId(transferId);
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.TRANSFER_ORDER_NOT_EXIST);
        }

        TransferOrderVO vo = convertToVO(order);
        cacheBucket.set(JSON.toJSONString(vo), 30, TimeUnit.MINUTES);

        return vo;
    }

    /**
     * 根据业务流水号查询转账订单
     * @param businessNo 业务流水号
     * @return 转账订单VO
     */
    @Override
    public TransferOrderVO getTransferOrderByBusinessNo(String businessNo) {
        log.debug("根据业务流水号查询转账订单, businessNo: {}", businessNo);

        TransferOrder order = transferOrderMapper.selectByBusinessNo(businessNo);
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.TRANSFER_ORDER_NOT_EXIST);
        }

        return convertToVO(order);
    }

    /**
     * 分页查询转账订单列表
     * 支持按转出账户、转入账户、状态、时间范围筛选
     * @param dto 查询条件DTO
     * @return 分页结果
     */
    @Override
    public Page<TransferOrderVO> queryTransferOrders(TransferQueryDTO dto) {
        log.info("查询转账订单列表, fromAccountId: {}, toAccountId: {}, status: {}",
                dto.getFromAccountId(), dto.getToAccountId(), dto.getStatus());

        // 参数校验：页码最小为1，每页最大条数限制
        int pageNum = Math.max(dto.getPageNum(), 1);
        int pageSize = Math.min(dto.getPageSize(), CommonConstants.MAX_PAGE_SIZE);

        Page<TransferOrder> page = new Page<>(pageNum, pageSize);
        Page<TransferOrder> resultPage = transferOrderMapper.selectByCondition(
                page,
                dto.getFromAccountId(),
                dto.getToAccountId(),
                dto.getStatus(),
                dto.getStartTime(),
                dto.getEndTime()
        );

        // 转换为VO
        Page<TransferOrderVO> voPage = new Page<>(pageNum, pageSize, resultPage.getTotal());
        List<TransferOrderVO> voList = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * 校验转账参数
     * @param dto 转账请求DTO
     */
    private void validateTransferParam(TransferDTO dto) {
        // 校验币种
        if (CurrencyEnum.getByCode(dto.getCurrency()) == null) {
            throw new BusinessException(ResultCodeEnum.CURRENCY_NOT_SUPPORTED);
        }
        // 校验金额
        AmountUtil.validateAmount(dto.getAmount());
    }

    /**
     * 校验账户状态
     * 检查账户是否存在、是否关闭、是否冻结、币种是否匹配
     * @param accountId 账户ID
     * @param currency 币种
     * @return 账户实体
     */
    private Account validateAccount(String accountId, String currency) {
        Account account = accountMapper.selectByAccountId(accountId);
        if (account == null) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST, "账户不存在: " + accountId);
        }
        if (AccountStatusEnum.CLOSED.getCode().equals(account.getStatus())) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_CLOSED, "账户已销户: " + accountId);
        }
        if (AccountStatusEnum.FROZEN.getCode().equals(account.getStatus())) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_FROZEN, "账户已冻结: " + accountId);
        }
        if (!currency.equals(account.getCurrency())) {
            throw new BusinessException(ResultCodeEnum.TRANSFER_CURRENCY_MISMATCH,
                    "账户币种不匹配, 账户ID: " + accountId + ", 账户币种: " + account.getCurrency() + ", 请求币种: " + currency);
        }
        return account;
    }

    /**
     * 校验账户余额是否充足
     * @param account 账户实体
     * @param amount 金额（元）
     */
    private void validateSufficientBalance(Account account, BigDecimal amount) {
        long amountFen = AmountUtil.yuanToFen(amount);
        if (account.getBalance() < amountFen) {
            throw new BusinessException(ResultCodeEnum.INSUFFICIENT_BALANCE,
                    "账户余额不足, accountId: " + account.getAccountId() +
                            ", 余额: " + AmountUtil.fenToYuan(account.getBalance()) +
                            ", 需要: " + amount);
        }
    }

    /**
     * 构建转账订单实体
     * @param dto 转账请求DTO
     * @param transferId 转账订单ID
     * @param transferNo 转账单号
     * @param fromAccount 扣款方账户
     * @param toAccount 收款方账户
     * @return 转账订单实体
     */
    private TransferOrder buildTransferOrder(TransferDTO dto, String transferId, String transferNo,
                                              Account fromAccount, Account toAccount) {
        TransferOrder order = new TransferOrder();
        order.setId(SnowflakeIdGenerator.nextId());
        order.setTransferId(transferId);
        order.setTransferNo(transferNo);
        order.setBusinessNo(dto.getBusinessNo());
        order.setRequestId(dto.getRequestId());
        order.setFromAccountId(dto.getFromAccountId());
        order.setFromAccountNo(fromAccount.getAccountNo());
        order.setToAccountId(dto.getToAccountId());
        order.setToAccountNo(toAccount.getAccountNo());
        order.setAmount(dto.getAmount());
        order.setCurrency(dto.getCurrency());
        order.setStatus(PaymentStatusEnum.PENDING.getCode());
        order.setRemark(dto.getRemark());
        order.setOperator(dto.getOperator());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setDeleted(0);
        return order;
    }

    /**
     * 创建转账会计交易
     * 资金流向：内部账户A（扣款方） → 内部账户B（收款方）
     * 会计分录：
     *   借：银行存款（收款方账户）  金额
     *   贷：银行存款（扣款方账户）  金额
     *
     * 说明：借记收款人账户表示余额增加，贷记付款人账户表示余额减少
     * 两个分录均使用"银行存款"科目，因为都是内部账户的资金变动
     *
     * @param dto 转账请求DTO
     * @param fromAccount 扣款方账户
     * @param toAccount 收款方账户
     * @param transferId 转账订单ID
     * @return 交易ID
     */
    private String createTransferTransaction(TransferDTO dto, Account fromAccount, Account toAccount, String transferId) {
        TransactionCreateDTO txDTO = new TransactionCreateDTO();
        txDTO.setRequestId(dto.getRequestId());
        txDTO.setBusinessNo(dto.getBusinessNo() + "_TX");
        txDTO.setTransactionType(TransactionTypeEnum.TRANSFER.getCode());
        txDTO.setCurrency(dto.getCurrency());
        txDTO.setTotalAmount(dto.getAmount());
        txDTO.setSummary(dto.getRemark() != null ? dto.getRemark() : "账户间转账");
        txDTO.setOperator(dto.getOperator());

        List<TransactionEntryDTO> entries = new ArrayList<>();

        // 借方分录：收款方账户 - 银行存款增加
        TransactionEntryDTO debitEntry = new TransactionEntryDTO();
        debitEntry.setAccountId(toAccount.getAccountId());
        debitEntry.setSubjectCode("1001");
        debitEntry.setSubjectName("银行存款");
        debitEntry.setDirection(DebitCreditEnum.DEBIT.getCode());
        debitEntry.setAmount(dto.getAmount());
        debitEntry.setSummary("转账入账-收款方");
        entries.add(debitEntry);

        // 贷方分录：扣款方账户 - 银行存款减少
        TransactionEntryDTO creditEntry = new TransactionEntryDTO();
        creditEntry.setAccountId(fromAccount.getAccountId());
        creditEntry.setSubjectCode("1001");
        creditEntry.setSubjectName("银行存款");
        creditEntry.setDirection(DebitCreditEnum.CREDIT.getCode());
        creditEntry.setAmount(dto.getAmount());
        creditEntry.setSummary("转账出账-付款方");
        entries.add(creditEntry);

        txDTO.setEntries(entries);

        var result = transactionService.createTransaction(txDTO);
        log.info("转账记账完成, transferId: {}, businessNo: {}, transactionId: {}",
                transferId, dto.getBusinessNo(), result.getTransactionId());
        return result.getTransactionId();
    }

    /**
     * 将转账订单实体转换为VO
     * @param order 转账订单实体
     * @return 转账订单VO
     */
    private TransferOrderVO convertToVO(TransferOrder order) {
        TransferOrderVO vo = new TransferOrderVO();
        BeanUtils.copyProperties(order, vo);

        PaymentStatusEnum statusEnum = PaymentStatusEnum.getByCode(order.getStatus());
        if (statusEnum != null) {
            vo.setStatusDesc(statusEnum.getDesc());
        }

        return vo;
    }

    /**
     * 发送转账事件到RocketMQ
     * 用于通知其他业务系统转账完成
     * @param vo 转账订单VO
     */
    private void sendTransferEvent(TransferOrderVO vo) {
        try {
            AccountEvent event = new AccountEvent();
            event.setEventId(SnowflakeIdGenerator.nextIdStr());
            event.setEventType(CommonConstants.ROCKETMQ_TAG_TRANSFER);
            event.setAccountId(vo.getFromAccountId());
            event.setAccountNo(vo.getFromAccountNo());
            event.setTransactionId(vo.getTransferId());
            event.setTransactionType(PaymentTypeEnum.TRANSFER.getCode());
            event.setBalance(vo.getAmount());
            event.setRequestId(vo.getRequestId());
            event.setOperator(vo.getOperator());
            event.setEventTime(LocalDateTime.now());

            String destination = CommonConstants.ROCKETMQ_TOPIC_ACCOUNT + ":"
                    + CommonConstants.ROCKETMQ_TAG_TRANSFER;
            rocketMQTemplate.send(destination, MessageBuilder.withPayload(event).build());

            log.info("发送转账事件成功, transferId: {}", vo.getTransferId());
        } catch (Exception e) {
            log.error("发送转账事件失败, transferId: {}", vo.getTransferId(), e);
        }

        try {
            messageProducerService.sendTransferSuccessMessage(vo);
        } catch (Exception e) {
            log.error("发送转账成功消息失败, transferId: {}", vo.getTransferId(), e);
        }
    }

    /**
     * 删除账户缓存
     * @param accountId 账户ID
     */
    @Override
    public TransferOrderVO crossBankTransfer(TransferDTO dto) {
        log.info("开始跨行转账, businessNo={}, from={}, to={}, channel={}, amount={}",
                dto.getBusinessNo(), dto.getFromAccountId(), dto.getToAccountId(),
                dto.getChannelCode(), dto.getAmount());

        String sagaId = distributedTransactionService.executeCrossBankTransferWithSaga(dto);

        TransferOrder order = transferOrderMapper.selectByBusinessNo(dto.getBusinessNo());
        if (order == null) {
            order = new TransferOrder();
            order.setTransferId("CROSS_" + sagaId);
            order.setBusinessNo(dto.getBusinessNo());
            order.setFromAccountId(dto.getFromAccountId());
            order.setToAccountId(dto.getToAccountId());
            order.setAmount(dto.getAmount());
            order.setCurrency(dto.getCurrency());
            order.setStatus(PaymentStatusEnum.SUCCESS.getCode());
        }

        TransferOrderVO vo = convertToVO(order);
        vo.setRemark("跨行转账处理中，Saga事务ID: " + sagaId);

        try {
            String callbackUrl = dto.getCallbackUrl();
            messageProducerService.sendTransferSuccessWithCallback(vo, callbackUrl, null);
        } catch (Exception e) {
            log.error("发送跨行转账成功消息及回调失败, sagaId={}", sagaId, e);
        }

        log.info("跨行转账提交完成, sagaId={}, businessNo={}", sagaId, dto.getBusinessNo());
        return vo;
    }

    private void deleteAccountCache(String accountId) {
        String cacheKey = CommonConstants.ACCOUNT_CACHE_PREFIX + accountId;
        redissonClient.getBucket(cacheKey).delete();
    }
}
