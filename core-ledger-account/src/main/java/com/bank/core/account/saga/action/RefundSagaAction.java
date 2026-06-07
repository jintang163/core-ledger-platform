package com.bank.core.account.saga.action;

import com.alibaba.fastjson.JSON;
import com.bank.core.account.entity.Account;
import com.bank.core.account.entity.PaymentOrder;
import com.bank.core.account.entity.Transaction;
import com.bank.core.account.entity.TransactionEntry;
import com.bank.core.account.mapper.AccountMapper;
import com.bank.core.account.mapper.PaymentOrderMapper;
import com.bank.core.account.mapper.TransactionEntryMapper;
import com.bank.core.account.mapper.TransactionMapper;
import com.bank.core.account.saga.SagaAction;
import com.bank.core.common.constants.CommonConstants;
import com.bank.core.common.enums.*;
import com.bank.core.common.exception.BusinessException;
import com.bank.core.common.utils.AmountUtil;
import com.bank.core.common.utils.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundSagaAction implements SagaAction {

    private final AccountMapper accountMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final TransactionMapper transactionMapper;
    private final TransactionEntryMapper entryMapper;
    private final RedissonClient redissonClient;

    @Override
    public String getServiceName() {
        return "refund";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean forward(String sagaId, Map<String, Object> params) {
        log.info("Saga正向操作-原路退款, sagaId={}, params={}", sagaId, params);

        String originalPaymentId = (String) params.get("originalPaymentId");
        String refundAccountId = (String) params.get("refundAccountId");
        BigDecimal amount = (BigDecimal) params.get("amount");
        String currency = (String) params.get("currency");
        String businessNo = (String) params.get("businessNo");
        String operator = (String) params.get("operator");
        String remark = (String) params.get("remark");

        String idempotentKey = "saga:refund:forward:" + sagaId;
        RBucket<Boolean> idempotentBucket = redissonClient.getBucket(idempotentKey);
        if (!idempotentBucket.setIfAbsent(true, 24, TimeUnit.HOURS)) {
            log.warn("Saga正向操作-原路退款幂等命中, sagaId={}", sagaId);
            return true;
        }

        PaymentOrder originalOrder = paymentOrderMapper.selectByPaymentId(originalPaymentId);
        if (originalOrder == null) {
            throw new BusinessException(ResultCodeEnum.PAYMENT_ORDER_NOT_EXIST, "原支付订单不存在");
        }

        if (!PaymentStatusEnum.SUCCESS.getCode().equals(originalOrder.getStatus())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "原支付订单状态不正确，无法退款");
        }

        if (originalOrder.getAmount().compareTo(amount) < 0) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "退款金额不能大于原支付金额");
        }

        Account clearingAccount = getClearingAccount(originalOrder.getChannelCode(), currency);

        long amountFen = AmountUtil.yuanToFen(amount);
        Account freshClearing = accountMapper.selectByAccountId(clearingAccount.getAccountId());
        if (freshClearing.getBalance() < amountFen) {
            throw new BusinessException(ResultCodeEnum.INSUFFICIENT_BALANCE, "清算账户余额不足，无法退款");
        }

        int updated = accountMapper.freezeBalance(
                clearingAccount.getAccountId(),
                amountFen,
                freshClearing.getVersion(),
                LocalDateTime.now()
        );

        if (updated == 0) {
            throw new BusinessException(ResultCodeEnum.CONCURRENT_UPDATE_FAILED, "清算账户资金冻结失败");
        }

        String refundPaymentId = SnowflakeIdGenerator.nextIdStr();
        PaymentOrder refundOrder = new PaymentOrder();
        refundOrder.setId(SnowflakeIdGenerator.nextId());
        refundOrder.setPaymentId(refundPaymentId);
        refundOrder.setPaymentNo(SnowflakeIdGenerator.generatePaymentNo());
        refundOrder.setPaymentType(PaymentTypeEnum.REFUND.getCode());
        refundOrder.setBusinessNo(businessNo);
        refundOrder.setRequestId((String) params.get("requestId"));
        refundOrder.setAccountId(refundAccountId);
        refundOrder.setAmount(amount);
        refundOrder.setCurrency(currency);
        refundOrder.setChannelCode(originalOrder.getChannelCode());
        refundOrder.setStatus(PaymentStatusEnum.PROCESSING.getCode());
        refundOrder.setChannelStatus(ChannelStatusEnum.PENDING.getCode());
        refundOrder.setRemark(remark != null ? remark : "原路退款");
        refundOrder.setOperator(operator);
        refundOrder.setCreateTime(LocalDateTime.now());
        refundOrder.setUpdateTime(LocalDateTime.now());
        refundOrder.setDeleted(0);
        paymentOrderMapper.insert(refundOrder);

        createRefundTransaction(refundPaymentId, originalOrder, refundAccountId,
                clearingAccount.getAccountId(), amount, currency, operator, businessNo);

        saveForwardContext(sagaId, refundAccountId, clearingAccount.getAccountId(),
                amountFen, refundPaymentId, originalPaymentId);

        log.info("Saga正向操作-原路退款完成, sagaId={}, refundPaymentId={}", sagaId, refundPaymentId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean compensate(String sagaId, Map<String, Object> params) {
        log.info("Saga补偿操作-原路退款回滚, sagaId={}", sagaId);

        String idempotentKey = "saga:refund:compensate:" + sagaId;
        RBucket<Boolean> idempotentBucket = redissonClient.getBucket(idempotentKey);
        if (!idempotentBucket.setIfAbsent(true, 24, TimeUnit.HOURS)) {
            log.warn("Saga补偿操作-原路退款幂等命中, sagaId={}", sagaId);
            return true;
        }

        String contextKey = "saga:refund:context:" + sagaId;
        String contextJson = (String) redissonClient.getBucket(contextKey).get();
        if (contextJson == null) {
            log.warn("Saga补偿操作-上下文不存在, 可能正向操作未执行, sagaId={}", sagaId);
            return true;
        }

        Map<String, Object> context = JSON.parseObject(contextJson, Map.class);
        String refundAccountId = (String) context.get("refundAccountId");
        String clearingAccountId = (String) context.get("clearingAccountId");
        Long amountFen = Long.valueOf(context.get("amountFen").toString());
        String refundPaymentId = (String) context.get("refundPaymentId");

        try {
            PaymentOrder refundOrder = paymentOrderMapper.selectByPaymentId(refundPaymentId);
            if (refundOrder != null) {
                refundOrder.setStatus(PaymentStatusEnum.FAILED.getCode());
                refundOrder.setChannelStatus(ChannelStatusEnum.CHANNEL_FAILED.getCode());
                refundOrder.setUpdateTime(LocalDateTime.now());
                paymentOrderMapper.updateById(refundOrder);
            }

            int unfreezeUpdated = accountMapper.unfreezeBalance(
                    clearingAccountId,
                    amountFen,
                    LocalDateTime.now()
            );

            if (unfreezeUpdated == 0) {
                log.warn("Saga补偿操作-清算账户解冻失败, 可能资金已解冻, sagaId={}", sagaId);
            }

            deleteAccountCache(clearingAccountId);
            deleteAccountCache(refundAccountId);

        } catch (Exception e) {
            log.error("Saga补偿操作-原路退款回滚异常, sagaId={}", sagaId, e);
        }

        log.info("Saga补偿操作-原路退款回滚完成, sagaId={}", sagaId);
        return true;
    }

    private Account getClearingAccount(String channelCode, String currency) {
        String clearingAccountId = ChannelCodeEnum.getClearingAccountId(channelCode);
        if (clearingAccountId == null) {
            throw new BusinessException(ResultCodeEnum.CHANNEL_NOT_SUPPORTED,
                    "渠道未配置清算账户: " + channelCode);
        }

        Account clearingAccount = accountMapper.selectByAccountId(clearingAccountId);
        if (clearingAccount == null) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST,
                    "渠道清算账户不存在: " + clearingAccountId);
        }

        if (!currency.equals(clearingAccount.getCurrency())) {
            throw new BusinessException(ResultCodeEnum.TRANSFER_CURRENCY_MISMATCH,
                    "清算账户币种不匹配");
        }

        return clearingAccount;
    }

    private void createRefundTransaction(String paymentId, PaymentOrder originalOrder,
                                          String refundAccountId, String clearingAccountId,
                                          BigDecimal amount, String currency, String operator, String businessNo) {
        String transactionId = SnowflakeIdGenerator.nextIdStr();

        Transaction transaction = new Transaction();
        transaction.setId(SnowflakeIdGenerator.nextId());
        transaction.setTransactionId(transactionId);
        transaction.setTransactionNo(SnowflakeIdGenerator.generateTransactionNo());
        transaction.setTransactionType(TransactionTypeEnum.REFUND.getCode());
        transaction.setBusinessNo(businessNo + "_TX");
        transaction.setTotalAmount(amount);
        transaction.setCurrency(currency);
        transaction.setVoucherNo(SnowflakeIdGenerator.generateVoucherNo());
        transaction.setSummary("原路退款");
        transaction.setStatus(TransactionStatusEnum.PENDING.getCode());
        transaction.setRequestId(originalOrder.getRequestId());
        transaction.setOperator(operator);
        transaction.setTransactionTime(LocalDateTime.now());
        transaction.setCreateTime(LocalDateTime.now());
        transaction.setUpdateTime(LocalDateTime.now());
        transaction.setDeleted(0);
        transactionMapper.insert(transaction);

        Account refundAccount = accountMapper.selectByAccountId(refundAccountId);

        List<TransactionEntry> entries = new ArrayList<>();

        TransactionEntry debitEntry = new TransactionEntry();
        debitEntry.setId(SnowflakeIdGenerator.nextId());
        debitEntry.setEntryId(SnowflakeIdGenerator.generateEntryId());
        debitEntry.setTransactionId(transactionId);
        debitEntry.setAccountId(clearingAccountId);
        debitEntry.setAccountNo(refundAccount.getAccountNo());
        debitEntry.setSubjectCode("2001");
        debitEntry.setSubjectName("清算账户-渠道往来");
        debitEntry.setDirection(DebitCreditEnum.CREDIT.getCode());
        debitEntry.setAmount(amount);
        debitEntry.setCurrency(currency);
        debitEntry.setSummary("退款-清算账户");
        debitEntry.setCreateTime(LocalDateTime.now());
        entries.add(debitEntry);

        TransactionEntry creditEntry = new TransactionEntry();
        creditEntry.setId(SnowflakeIdGenerator.nextId());
        creditEntry.setEntryId(SnowflakeIdGenerator.generateEntryId());
        creditEntry.setTransactionId(transactionId);
        creditEntry.setAccountId(refundAccountId);
        creditEntry.setAccountNo(refundAccount.getAccountNo());
        creditEntry.setSubjectCode("1001");
        creditEntry.setSubjectName("银行存款");
        creditEntry.setDirection(DebitCreditEnum.DEBIT.getCode());
        creditEntry.setAmount(amount);
        creditEntry.setCurrency(currency);
        creditEntry.setSummary("退款-用户账户");
        creditEntry.setCreateTime(LocalDateTime.now());
        entries.add(creditEntry);

        for (TransactionEntry entry : entries) {
            entryMapper.insert(entry);
        }

        transaction.setStatus(TransactionStatusEnum.SUCCESS.getCode());
        transactionMapper.updateById(transaction);

        int debitUpdated = accountMapper.updateBalanceWithVersion(
                clearingAccountId,
                -AmountUtil.yuanToFen(amount),
                accountMapper.selectByAccountId(clearingAccountId).getVersion(),
                LocalDateTime.now()
        );

        int creditUpdated = accountMapper.updateBalanceWithVersion(
                refundAccountId,
                AmountUtil.yuanToFen(amount),
                accountMapper.selectByAccountId(refundAccountId).getVersion(),
                LocalDateTime.now()
        );

        if (debitUpdated == 0 || creditUpdated == 0) {
            throw new BusinessException(ResultCodeEnum.CONCURRENT_UPDATE_FAILED, "退款记账更新余额失败");
        }
    }

    private void saveForwardContext(String sagaId, String refundAccountId, String clearingAccountId,
                                     Long amountFen, String refundPaymentId, String originalPaymentId) {
        Map<String, Object> context = new HashMap<>();
        context.put("refundAccountId", refundAccountId);
        context.put("clearingAccountId", clearingAccountId);
        context.put("amountFen", amountFen);
        context.put("refundPaymentId", refundPaymentId);
        context.put("originalPaymentId", originalPaymentId);

        String contextKey = "saga:refund:context:" + sagaId;
        redissonClient.getBucket(contextKey).set(JSON.toJSONString(context), 24, TimeUnit.HOURS);
    }

    private void deleteAccountCache(String accountId) {
        String cacheKey = CommonConstants.ACCOUNT_CACHE_PREFIX + accountId;
        redissonClient.getBucket(cacheKey).delete();
    }
}
