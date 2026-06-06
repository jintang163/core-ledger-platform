package com.bank.core.account.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bank.core.account.entity.Account;
import com.bank.core.account.entity.BatchTransferItem;
import com.bank.core.account.entity.BatchTransferOrder;
import com.bank.core.account.mapper.AccountMapper;
import com.bank.core.account.mapper.BatchTransferItemMapper;
import com.bank.core.account.mapper.BatchTransferOrderMapper;
import com.bank.core.account.service.BatchTransferService;
import com.bank.core.api.dto.BatchTransferDTO;
import com.bank.core.api.dto.BatchTransferItemDTO;
import com.bank.core.api.dto.BatchTransferQueryDTO;
import com.bank.core.api.dto.TransactionCreateDTO;
import com.bank.core.api.dto.TransactionEntryDTO;
import com.bank.core.api.event.AccountEvent;
import com.bank.core.api.vo.BatchTransferItemVO;
import com.bank.core.api.vo.BatchTransferOrderVO;
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
 * 批量转账服务实现类
 * 处理批量转账（代发）业务，支持一次请求包含多条转账记录，支持部分成功
 *
 * 核心业务逻辑：
 * 1. 批量转账：内部账户A → 多个内部账户B（收款方）
 * 2. 支持部分成功：单条明细失败不影响其他明细
 * 3. 先校验扣款方余额是否足够（按总金额校验）
 *
 * 技术要点：
 * - 全链路幂等性校验（Redis + DB + 分布式锁）
 * - 分布式锁防止并发操作
 * - 借贷记账法：每条明细单独记账
 * - 状态管理：待处理 → 处理中 → 成功/部分成功/失败
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchTransferServiceImpl implements BatchTransferService {

    private final BatchTransferOrderMapper batchTransferOrderMapper;
    private final BatchTransferItemMapper batchTransferItemMapper;
    private final AccountMapper accountMapper;
    private final TransactionServiceImpl transactionService;
    private final RedissonClient redissonClient;
    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 批量转账（代发）
     * 资金流向：内部账户A（扣款方） → 多个内部账户B（收款方）
     *
     * 业务流程：
     * 1. 幂等性校验（Redis + DB + 分布式锁三层校验）
     * 2. 参数校验（币种、金额、明细数量）
     * 3. 计算总金额，校验扣款方余额是否足够
     * 4. 分布式锁保证并发安全
     * 5. 创建批量转账订单（状态：待处理）
     * 6. 创建所有转账明细（状态：待处理）
     * 7. 更新订单状态为处理中
     * 8. 逐个处理转账明细（支持部分成功）
     *    - 每条明细单独创建会计交易
     *    - 单条失败不影响其他明细
     *    - 记录成功/失败数量和金额
     * 9. 根据处理结果更新订单状态（成功/部分成功/失败）
     * 10. 发送批量转账事件到MQ
     * 11. 清除账户缓存
     *
     * @param dto 批量转账请求DTO
     * @return 批量转账订单VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchTransferOrderVO batchTransfer(BatchTransferDTO dto) {
        log.info("开始批量转账, businessNo: {}, fromAccountId: {}, itemCount: {}",
                dto.getBusinessNo(), dto.getFromAccountId(), dto.getItems().size());

        IdempotentUtil.checkIdempotent(dto.getRequestId());

        validateBatchParam(dto);

        String idempotentKey = CommonConstants.BATCH_TRANSFER_IDEMPOTENT_PREFIX + dto.getBusinessNo();
        RBucket<String> idempotentBucket = redissonClient.getBucket(idempotentKey);
        String cachedBatchId = idempotentBucket.get();
        if (cachedBatchId != null) {
            log.warn("幂等命中, businessNo: {}, batchId: {}", dto.getBusinessNo(), cachedBatchId);
            BatchTransferOrder existOrder = batchTransferOrderMapper.selectByBatchId(cachedBatchId);
            if (existOrder != null) {
                return convertToVO(existOrder, true);
            }
        }

        BatchTransferOrder existOrder = batchTransferOrderMapper.selectByBusinessNo(dto.getBusinessNo());
        if (existOrder != null) {
            log.warn("业务单号已存在, businessNo: {}", dto.getBusinessNo());
            idempotentBucket.set(existOrder.getBatchId(), 24, TimeUnit.HOURS);
            return convertToVO(existOrder, true);
        }

        String lockKey = CommonConstants.BATCH_TRANSFER_LOCK_PREFIX + dto.getBusinessNo();
        return DistributedLockUtil.executeWithLock(lockKey, () -> {
            BatchTransferOrder existAgain = batchTransferOrderMapper.selectByBusinessNo(dto.getBusinessNo());
            if (existAgain != null) {
                log.warn("分布式锁内二次检查发现业务单号已存在, businessNo: {}", dto.getBusinessNo());
                idempotentBucket.set(existAgain.getBatchId(), 24, TimeUnit.HOURS);
                return convertToVO(existAgain, true);
            }

            Account fromAccount = validateAccount(dto.getFromAccountId(), dto.getCurrency());

            BigDecimal totalAmount = dto.getItems().stream()
                    .map(BatchTransferItemDTO::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            validateSufficientBalance(fromAccount, totalAmount);

            String batchId = SnowflakeIdGenerator.nextIdStr();
            String batchNo = SnowflakeIdGenerator.generateBatchNo();

            BatchTransferOrder order = buildBatchOrder(dto, batchId, batchNo, fromAccount, totalAmount);
            batchTransferOrderMapper.insert(order);

            List<BatchTransferItem> items = buildBatchItems(dto.getItems(), batchId, fromAccount);
            for (BatchTransferItem item : items) {
                batchTransferItemMapper.insert(item);
            }

            order.setStatus(PaymentStatusEnum.PROCESSING.getCode());
            order.setUpdateTime(LocalDateTime.now());
            batchTransferOrderMapper.updateById(order);

            int successCount = 0;
            int failCount = 0;
            BigDecimal successAmount = BigDecimal.ZERO;
            BigDecimal failAmount = BigDecimal.ZERO;

            for (BatchTransferItem item : items) {
                try {
                    String transactionId = processSingleTransfer(item, fromAccount, dto);
                    item.setStatus(PaymentStatusEnum.SUCCESS.getCode());
                    item.setTransactionId(transactionId);
                    item.setFinishTime(LocalDateTime.now());
                    item.setUpdateTime(LocalDateTime.now());
                    batchTransferItemMapper.updateById(item);
                    successCount++;
                    successAmount = successAmount.add(item.getAmount());
                    log.info("批量转账明细成功, batchId: {}, itemId: {}, toAccountId: {}, amount: {}",
                            batchId, item.getItemId(), item.getToAccountId(), item.getAmount());
                } catch (Exception e) {
                    log.error("批量转账明细失败, batchId: {}, itemId: {}, toAccountId: {}, amount: {}",
                            batchId, item.getItemId(), item.getToAccountId(), item.getAmount(), e);
                    item.setStatus(PaymentStatusEnum.FAILED.getCode());
                    item.setFailReason(e.getMessage());
                    item.setFinishTime(LocalDateTime.now());
                    item.setUpdateTime(LocalDateTime.now());
                    batchTransferItemMapper.updateById(item);
                    failCount++;
                    failAmount = failAmount.add(item.getAmount());
                }
            }

            Integer finalStatus;
            if (successCount == items.size()) {
                finalStatus = PaymentStatusEnum.SUCCESS.getCode();
            } else if (successCount > 0) {
                finalStatus = PaymentStatusEnum.PARTIAL_SUCCESS.getCode();
            } else {
                finalStatus = PaymentStatusEnum.FAILED.getCode();
            }

            order.setStatus(finalStatus);
            order.setSuccessCount(successCount);
            order.setSuccessAmount(successAmount);
            order.setFailCount(failCount);
            order.setFailAmount(failAmount);
            order.setFinishTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());
            batchTransferOrderMapper.updateById(order);

            BatchTransferOrderVO vo = convertToVO(order, items);

            idempotentBucket.set(batchId, 24, TimeUnit.HOURS);

            sendBatchTransferEvent(vo);

            deleteAccountCache(dto.getFromAccountId());
            for (BatchTransferItemDTO itemDTO : dto.getItems()) {
                deleteAccountCache(itemDTO.getToAccountId());
            }

            log.info("批量转账完成, batchId: {}, total: {}, success: {}, fail: {}, status: {}",
                    batchId, items.size(), successCount, failCount, finalStatus);
            return vo;
        });
    }

    /**
     * 根据批量转账ID查询批量转账订单
     * 先查缓存，缓存不存在再查数据库，查询结果写入缓存
     * @param batchId 批量转账订单ID
     * @return 批量转账订单VO
     */
    @Override
    public BatchTransferOrderVO getBatchTransferOrder(String batchId) {
        log.debug("查询批量转账订单, batchId: {}", batchId);

        String cacheKey = CommonConstants.BATCH_TRANSFER_CACHE_PREFIX + batchId;
        RBucket<String> cacheBucket = redissonClient.getBucket(cacheKey);
        String cacheValue = cacheBucket.get();
        if (cacheValue != null) {
            BatchTransferOrderVO vo = JSON.parseObject(cacheValue, BatchTransferOrderVO.class);
            log.debug("从缓存获取批量转账订单成功, batchId: {}", batchId);
            return vo;
        }

        BatchTransferOrder order = batchTransferOrderMapper.selectByBatchId(batchId);
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.BATCH_TRANSFER_ORDER_NOT_EXIST);
        }

        BatchTransferOrderVO vo = convertToVO(order, true);
        cacheBucket.set(JSON.toJSONString(vo), 30, TimeUnit.MINUTES);

        return vo;
    }

    /**
     * 根据业务流水号查询批量转账订单
     * @param businessNo 业务流水号
     * @return 批量转账订单VO
     */
    @Override
    public BatchTransferOrderVO getBatchTransferOrderByBusinessNo(String businessNo) {
        log.debug("根据业务流水号查询批量转账订单, businessNo: {}", businessNo);

        BatchTransferOrder order = batchTransferOrderMapper.selectByBusinessNo(businessNo);
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.BATCH_TRANSFER_ORDER_NOT_EXIST);
        }

        return convertToVO(order, true);
    }

    /**
     * 查询批量转账明细列表
     * @param batchId 批量转账订单ID
     * @return 明细VO列表
     */
    @Override
    public List<BatchTransferItemVO> getBatchTransferItems(String batchId) {
        log.debug("查询批量转账明细, batchId: {}", batchId);

        BatchTransferOrder order = batchTransferOrderMapper.selectByBatchId(batchId);
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.BATCH_TRANSFER_ORDER_NOT_EXIST);
        }

        List<BatchTransferItem> items = batchTransferItemMapper.selectByBatchId(batchId);
        return items.stream()
                .map(this::convertItemToVO)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询批量转账订单列表
     * 支持按扣款方账户、状态、时间范围筛选
     * @param dto 查询条件DTO
     * @return 分页结果
     */
    @Override
    public Page<BatchTransferOrderVO> queryBatchTransferOrders(BatchTransferQueryDTO dto) {
        log.info("查询批量转账订单列表, fromAccountId: {}, status: {}",
                dto.getFromAccountId(), dto.getStatus());

        // 参数校验：页码最小为1，每页最大条数限制
        int pageNum = Math.max(dto.getPageNum(), 1);
        int pageSize = Math.min(dto.getPageSize(), CommonConstants.MAX_PAGE_SIZE);

        Page<BatchTransferOrder> page = new Page<>(pageNum, pageSize);
        Page<BatchTransferOrder> resultPage = batchTransferOrderMapper.selectByCondition(
                page,
                dto.getFromAccountId(),
                dto.getStatus(),
                dto.getStartTime(),
                dto.getEndTime()
        );

        // 转换为VO
        Page<BatchTransferOrderVO> voPage = new Page<>(pageNum, pageSize, resultPage.getTotal());
        List<BatchTransferOrderVO> voList = resultPage.getRecords().stream()
                .map(order -> convertToVO(order, false))
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * 校验批量转账参数
     * @param dto 批量转账请求DTO
     */
    private void validateBatchParam(BatchTransferDTO dto) {
        // 校验明细不为空
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException(ResultCodeEnum.BATCH_TRANSFER_EMPTY);
        }
        // 校验明细数量不超过最大限制
        if (dto.getItems().size() > CommonConstants.MAX_BATCH_TRANSFER_SIZE) {
            throw new BusinessException(ResultCodeEnum.BATCH_TRANSFER_TOO_MANY_ITEMS,
                    "最大支持" + CommonConstants.MAX_BATCH_TRANSFER_SIZE + "条明细");
        }
        // 校验币种
        if (CurrencyEnum.getByCode(dto.getCurrency()) == null) {
            throw new BusinessException(ResultCodeEnum.CURRENCY_NOT_SUPPORTED);
        }
        // 校验每条明细的金额和账户
        for (BatchTransferItemDTO item : dto.getItems()) {
            AmountUtil.validateAmount(item.getAmount());
            if (dto.getFromAccountId().equals(item.getToAccountId())) {
                throw new BusinessException(ResultCodeEnum.TRANSFER_SAME_ACCOUNT,
                        "付款方和收款方不能相同, toAccountId: " + item.getToAccountId());
            }
        }
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
     * 构建批量转账订单实体
     * @param dto 批量转账请求DTO
     * @param batchId 批量转账订单ID
     * @param batchNo 批量转账单号
     * @param fromAccount 扣款方账户
     * @param totalAmount 总金额
     * @return 批量转账订单实体
     */
    private BatchTransferOrder buildBatchOrder(BatchTransferDTO dto, String batchId, String batchNo,
                                                Account fromAccount, BigDecimal totalAmount) {
        BatchTransferOrder order = new BatchTransferOrder();
        order.setId(SnowflakeIdGenerator.nextId());
        order.setBatchId(batchId);
        order.setBatchNo(batchNo);
        order.setBusinessNo(dto.getBusinessNo());
        order.setRequestId(dto.getRequestId());
        order.setFromAccountId(dto.getFromAccountId());
        order.setFromAccountNo(fromAccount.getAccountNo());
        order.setCurrency(dto.getCurrency());
        order.setTotalCount(dto.getItems().size());
        order.setTotalAmount(totalAmount);
        order.setSuccessCount(0);
        order.setSuccessAmount(BigDecimal.ZERO);
        order.setFailCount(0);
        order.setFailAmount(BigDecimal.ZERO);
        order.setStatus(PaymentStatusEnum.PENDING.getCode());
        order.setRemark(dto.getRemark());
        order.setOperator(dto.getOperator());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        order.setDeleted(0);
        return order;
    }

    /**
     * 构建批量转账明细实体列表
     * @param itemDTOs 明细DTO列表
     * @param batchId 批量转账订单ID
     * @param fromAccount 扣款方账户
     * @return 明细实体列表
     */
    private List<BatchTransferItem> buildBatchItems(List<BatchTransferItemDTO> itemDTOs, String batchId, Account fromAccount) {
        List<BatchTransferItem> items = new ArrayList<>();
        for (BatchTransferItemDTO itemDTO : itemDTOs) {
            BatchTransferItem item = new BatchTransferItem();
            item.setId(SnowflakeIdGenerator.nextId());
            item.setItemId(SnowflakeIdGenerator.generateBatchItemId());
            item.setBatchId(batchId);
            item.setTransferId(SnowflakeIdGenerator.nextIdStr());
            item.setToAccountId(itemDTO.getToAccountId());

            Account toAccount = accountMapper.selectByAccountId(itemDTO.getToAccountId());
            if (toAccount != null) {
                item.setToAccountNo(toAccount.getAccountNo());
            }

            item.setAmount(itemDTO.getAmount());
            item.setStatus(PaymentStatusEnum.PENDING.getCode());
            item.setRemark(itemDTO.getRemark());
            item.setCreateTime(LocalDateTime.now());
            item.setUpdateTime(LocalDateTime.now());
            items.add(item);
        }
        return items;
    }

    /**
     * 处理单条批量转账明细
     * 资金流向：扣款方账户 → 收款方账户
     * 会计分录：
     *   借：银行存款（收款方账户）  金额
     *   贷：银行存款（扣款方账户）  金额
     *
     * 说明：
     * - 每条明细单独创建会计交易
     * - 单条失败不影响其他明细（通过try-catch实现部分成功）
     * - 每次处理后更新扣款方账户的余额和版本号，用于后续明细的余额校验
     *
     * @param item 转账明细实体
     * @param fromAccount 扣款方账户
     * @param dto 批量转账请求DTO
     * @return 交易ID
     */
    private String processSingleTransfer(BatchTransferItem item, Account fromAccount, BatchTransferDTO dto) {
        // 校验收款方账户状态
        Account toAccount = validateAccount(item.getToAccountId(), dto.getCurrency());

        // 校验扣款方余额是否足够（每次都重新校验，因为之前的明细已经扣减了余额）
        validateSufficientBalance(fromAccount, item.getAmount());

        // 构建会计交易DTO
        TransactionCreateDTO txDTO = new TransactionCreateDTO();
        txDTO.setRequestId(dto.getRequestId() + "_" + item.getItemId());
        txDTO.setBusinessNo(dto.getBusinessNo() + "_" + item.getItemId() + "_TX");
        txDTO.setTransactionType(TransactionTypeEnum.TRANSFER.getCode());
        txDTO.setCurrency(dto.getCurrency());
        txDTO.setTotalAmount(item.getAmount());
        txDTO.setSummary(item.getRemark() != null ? item.getRemark() : "批量转账");
        txDTO.setOperator(dto.getOperator());

        List<TransactionEntryDTO> entries = new ArrayList<>();

        // 借方分录：收款方账户 - 银行存款增加
        TransactionEntryDTO debitEntry = new TransactionEntryDTO();
        debitEntry.setAccountId(toAccount.getAccountId());
        debitEntry.setSubjectCode("1001");
        debitEntry.setSubjectName("银行存款");
        debitEntry.setDirection(DebitCreditEnum.DEBIT.getCode());
        debitEntry.setAmount(item.getAmount());
        debitEntry.setSummary("批量转账入账-收款方");
        entries.add(debitEntry);

        // 贷方分录：扣款方账户 - 银行存款减少
        TransactionEntryDTO creditEntry = new TransactionEntryDTO();
        creditEntry.setAccountId(fromAccount.getAccountId());
        creditEntry.setSubjectCode("1001");
        creditEntry.setSubjectName("银行存款");
        creditEntry.setDirection(DebitCreditEnum.CREDIT.getCode());
        creditEntry.setAmount(item.getAmount());
        creditEntry.setSummary("批量转账出账-付款方");
        entries.add(creditEntry);

        txDTO.setEntries(entries);

        // 创建会计交易
        var result = transactionService.createTransaction(txDTO);

        // 更新扣款方账户的余额和版本号（从数据库重新查询，保证后续明细校验正确）
        Account freshFromAccount = accountMapper.selectByAccountId(fromAccount.getAccountId());
        fromAccount.setBalance(freshFromAccount.getBalance());
        fromAccount.setVersion(freshFromAccount.getVersion());

        return result.getTransactionId();
    }

    /**
     * 将批量转账订单实体转换为VO（可选是否包含明细）
     * @param order 批量转账订单实体
     * @param withItems 是否包含明细
     * @return 批量转账订单VO
     */
    private BatchTransferOrderVO convertToVO(BatchTransferOrder order, boolean withItems) {
        List<BatchTransferItem> items = null;
        if (withItems) {
            items = batchTransferItemMapper.selectByBatchId(order.getBatchId());
        }
        return convertToVO(order, items);
    }

    /**
     * 将批量转账订单实体转换为VO（包含明细）
     * @param order 批量转账订单实体
     * @param items 明细列表
     * @return 批量转账订单VO
     */
    private BatchTransferOrderVO convertToVO(BatchTransferOrder order, List<BatchTransferItem> items) {
        BatchTransferOrderVO vo = new BatchTransferOrderVO();
        BeanUtils.copyProperties(order, vo);

        // 转换状态描述
        PaymentStatusEnum statusEnum = PaymentStatusEnum.getByCode(order.getStatus());
        if (statusEnum != null) {
            vo.setStatusDesc(statusEnum.getDesc());
        }

        // 转换明细
        if (items != null && !items.isEmpty()) {
            List<BatchTransferItemVO> itemVOs = items.stream()
                    .map(this::convertItemToVO)
                    .collect(Collectors.toList());
            vo.setItems(itemVOs);
        }

        return vo;
    }

    /**
     * 将批量转账明细实体转换为VO
     * @param item 批量转账明细实体
     * @return 批量转账明细VO
     */
    private BatchTransferItemVO convertItemToVO(BatchTransferItem item) {
        BatchTransferItemVO vo = new BatchTransferItemVO();
        BeanUtils.copyProperties(item, vo);

        // 转换状态描述
        PaymentStatusEnum statusEnum = PaymentStatusEnum.getByCode(item.getStatus());
        if (statusEnum != null) {
            vo.setStatusDesc(statusEnum.getDesc());
        }

        return vo;
    }

    /**
     * 发送批量转账事件到RocketMQ
     * 用于通知其他业务系统批量转账完成
     * @param vo 批量转账订单VO
     */
    private void sendBatchTransferEvent(BatchTransferOrderVO vo) {
        try {
            AccountEvent event = new AccountEvent();
            event.setEventId(SnowflakeIdGenerator.nextIdStr());
            event.setEventType(CommonConstants.ROCKETMQ_TAG_BATCH_TRANSFER);
            event.setAccountId(vo.getFromAccountId());
            event.setAccountNo(vo.getFromAccountNo());
            event.setTransactionId(vo.getBatchId());
            event.setTransactionType(PaymentTypeEnum.BATCH_TRANSFER.getCode());
            event.setBalance(vo.getTotalAmount());
            event.setRequestId(vo.getRequestId());
            event.setOperator(vo.getOperator());
            event.setEventTime(LocalDateTime.now());

            String destination = CommonConstants.ROCKETMQ_TOPIC_ACCOUNT + ":"
                    + CommonConstants.ROCKETMQ_TAG_BATCH_TRANSFER;
            rocketMQTemplate.send(destination, MessageBuilder.withPayload(event).build());

            log.info("发送批量转账事件成功, batchId: {}", vo.getBatchId());
        } catch (Exception e) {
            log.error("发送批量转账事件失败, batchId: {}", vo.getBatchId(), e);
        }
    }

    /**
     * 删除账户缓存
     * @param accountId 账户ID
     */
    private void deleteAccountCache(String accountId) {
        String cacheKey = CommonConstants.ACCOUNT_CACHE_PREFIX + accountId;
        redissonClient.getBucket(cacheKey).delete();
    }
}
