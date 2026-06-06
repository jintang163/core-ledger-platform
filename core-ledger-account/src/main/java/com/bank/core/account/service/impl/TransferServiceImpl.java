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

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final TransferOrderMapper transferOrderMapper;
    private final AccountMapper accountMapper;
    private final TransactionServiceImpl transactionService;
    private final RedissonClient redissonClient;
    private final RocketMQTemplate rocketMQTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransferOrderVO transfer(TransferDTO dto) {
        log.info("开始账户间转账, businessNo: {}, fromAccountId: {}, toAccountId: {}, amount: {}",
                dto.getBusinessNo(), dto.getFromAccountId(), dto.getToAccountId(), dto.getAmount());

        IdempotentUtil.checkIdempotent(dto.getRequestId());

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

    @Override
    public TransferOrderVO getTransferOrderByBusinessNo(String businessNo) {
        log.debug("根据业务流水号查询转账订单, businessNo: {}", businessNo);

        TransferOrder order = transferOrderMapper.selectByBusinessNo(businessNo);
        if (order == null) {
            throw new BusinessException(ResultCodeEnum.TRANSFER_ORDER_NOT_EXIST);
        }

        return convertToVO(order);
    }

    @Override
    public Page<TransferOrderVO> queryTransferOrders(TransferQueryDTO dto) {
        log.info("查询转账订单列表, fromAccountId: {}, toAccountId: {}, status: {}",
                dto.getFromAccountId(), dto.getToAccountId(), dto.getStatus());

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

        Page<TransferOrderVO> voPage = new Page<>(pageNum, pageSize, resultPage.getTotal());
        List<TransferOrderVO> voList = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    private void validateTransferParam(TransferDTO dto) {
        if (CurrencyEnum.getByCode(dto.getCurrency()) == null) {
            throw new BusinessException(ResultCodeEnum.CURRENCY_NOT_SUPPORTED);
        }
        AmountUtil.validateAmount(dto.getAmount());
    }

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

    private void validateSufficientBalance(Account account, BigDecimal amount) {
        long amountFen = AmountUtil.yuanToFen(amount);
        if (account.getBalance() < amountFen) {
            throw new BusinessException(ResultCodeEnum.INSUFFICIENT_BALANCE,
                    "账户余额不足, accountId: " + account.getAccountId() +
                            ", 余额: " + AmountUtil.fenToYuan(account.getBalance()) +
                            ", 需要: " + amount);
        }
    }

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
        TransactionEntryDTO debitEntry = new TransactionEntryDTO();
        debitEntry.setAccountId(toAccount.getAccountId());
        debitEntry.setSubjectCode("1001");
        debitEntry.setSubjectName("银行存款");
        debitEntry.setDirection(DebitCreditEnum.DEBIT.getCode());
        debitEntry.setAmount(dto.getAmount());
        debitEntry.setSummary("转账入账");
        entries.add(debitEntry);

        TransactionEntryDTO creditEntry = new TransactionEntryDTO();
        creditEntry.setAccountId(fromAccount.getAccountId());
        creditEntry.setSubjectCode("1001");
        creditEntry.setSubjectName("银行存款");
        creditEntry.setDirection(DebitCreditEnum.CREDIT.getCode());
        creditEntry.setAmount(dto.getAmount());
        creditEntry.setSummary("转账出账");
        entries.add(creditEntry);

        txDTO.setEntries(entries);

        var result = transactionService.createTransaction(txDTO);
        log.info("转账记账完成, transferId: {}, businessNo: {}, transactionId: {}",
                transferId, dto.getBusinessNo(), result.getTransactionId());
        return result.getTransactionId();
    }

    private TransferOrderVO convertToVO(TransferOrder order) {
        TransferOrderVO vo = new TransferOrderVO();
        BeanUtils.copyProperties(order, vo);

        PaymentStatusEnum statusEnum = PaymentStatusEnum.getByCode(order.getStatus());
        if (statusEnum != null) {
            vo.setStatusDesc(statusEnum.getDesc());
        }

        return vo;
    }

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
    }

    private void deleteAccountCache(String accountId) {
        String cacheKey = CommonConstants.ACCOUNT_CACHE_PREFIX + accountId;
        redissonClient.getBucket(cacheKey).delete();
    }
}
