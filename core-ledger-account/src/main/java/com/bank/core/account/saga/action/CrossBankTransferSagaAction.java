package com.bank.core.account.saga.action;

import com.alibaba.fastjson.JSON;
import com.bank.core.account.channel.ChannelAdapter;
import com.bank.core.account.channel.ChannelAdapterFactory;
import com.bank.core.account.channel.ChannelNotificationRequest;
import com.bank.core.account.channel.ChannelNotificationResponse;
import com.bank.core.account.entity.Account;
import com.bank.core.account.entity.PaymentOrder;
import com.bank.core.account.mapper.AccountMapper;
import com.bank.core.account.mapper.PaymentOrderMapper;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrossBankTransferSagaAction implements SagaAction {

    private final AccountMapper accountMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final ChannelAdapterFactory channelAdapterFactory;
    private final RedissonClient redissonClient;

    @Override
    public String getServiceName() {
        return "crossBankTransfer";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean forward(String sagaId, Map<String, Object> params) {
        log.info("Saga正向操作-跨行转账, sagaId={}, params={}", sagaId, params);

        String fromAccountId = (String) params.get("fromAccountId");
        String toAccountId = (String) params.get("toAccountId");
        BigDecimal amount = (BigDecimal) params.get("amount");
        String currency = (String) params.get("currency");
        String channelCode = (String) params.get("channelCode");
        String businessNo = (String) params.get("businessNo");
        String operator = (String) params.get("operator");
        String remark = (String) params.get("remark");

        String idempotentKey = "saga:crossbank:forward:" + sagaId;
        RBucket<Boolean> idempotentBucket = redissonClient.getBucket(idempotentKey);
        if (!idempotentBucket.setIfAbsent(true, 24, TimeUnit.HOURS)) {
            log.warn("Saga正向操作-跨行转账幂等命中, sagaId={}", sagaId);
            return true;
        }

        Account fromAccount = accountMapper.selectByAccountId(fromAccountId);
        if (fromAccount == null) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST, "付款账户不存在");
        }

        if (AccountStatusEnum.FROZEN.getCode().equals(fromAccount.getStatus())
                || AccountStatusEnum.CLOSED.getCode().equals(fromAccount.getStatus())) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_STATUS_ERROR, "付款账户状态异常");
        }

        long amountFen = AmountUtil.yuanToFen(amount);
        Account freshFrom = accountMapper.selectByAccountId(fromAccountId);
        if (freshFrom.getBalance() < amountFen) {
            throw new BusinessException(ResultCodeEnum.INSUFFICIENT_BALANCE);
        }

        int updated = accountMapper.freezeBalance(
                fromAccountId,
                amountFen,
                freshFrom.getVersion(),
                LocalDateTime.now()
        );

        if (updated == 0) {
            throw new BusinessException(ResultCodeEnum.CONCURRENT_UPDATE_FAILED, "资金冻结失败");
        }

        String paymentId = (String) params.get("paymentId");
        if (paymentId == null) {
            paymentId = SnowflakeIdGenerator.nextIdStr();
        }

        PaymentOrder order = paymentOrderMapper.selectByPaymentId(paymentId);
        if (order == null) {
            order = new PaymentOrder();
            order.setId(SnowflakeIdGenerator.nextId());
            order.setPaymentId(paymentId);
            order.setPaymentNo(SnowflakeIdGenerator.generatePaymentNo());
            order.setPaymentType(PaymentTypeEnum.CROSS_BANK_TRANSFER.getCode());
            order.setBusinessNo(businessNo);
            order.setRequestId((String) params.get("requestId"));
            order.setAccountId(fromAccountId);
            order.setAccountNo(fromAccount.getAccountNo());
            order.setAmount(amount);
            order.setCurrency(currency);
            order.setChannelCode(channelCode);
            order.setStatus(PaymentStatusEnum.PROCESSING.getCode());
            order.setChannelStatus(ChannelStatusEnum.PENDING.getCode());
            order.setRemark(remark);
            order.setOperator(operator);
            order.setCreateTime(LocalDateTime.now());
            order.setUpdateTime(LocalDateTime.now());
            order.setDeleted(0);
            paymentOrderMapper.insert(order);
        }

        try {
            ChannelAdapter adapter = channelAdapterFactory.getAdapter(channelCode);
            ChannelNotificationRequest request = ChannelNotificationRequest.builder()
                    .paymentId(paymentId)
                    .businessNo(businessNo)
                    .channelCode(channelCode)
                    .paymentType(PaymentTypeEnum.CROSS_BANK_TRANSFER.getCode())
                    .amount(amount)
                    .currency(currency)
                    .accountId(fromAccountId)
                    .accountNo(fromAccount.getAccountNo())
                    .channelStatus(ChannelStatusEnum.PENDING.getCode().toString())
                    .remark(remark)
                    .operator(operator)
                    .requestTime(LocalDateTime.now())
                    .build();

            request.getExtParams().put("toAccountId", toAccountId);

            ChannelNotificationResponse response = adapter.notifyCrossBankTransfer(request);

            if (!response.isSuccess()) {
                throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR,
                        "渠道处理失败: " + response.getResponseMessage());
            }

            if (response.getChannelOrderNo() != null) {
                order.setChannelOrderNo(response.getChannelOrderNo());
                order.setChannelStatus(ChannelStatusEnum.PROCESSING.getCode());
                paymentOrderMapper.updateById(order);
            }

            saveForwardContext(sagaId, fromAccountId, toAccountId, amountFen, paymentId, channelCode);

            log.info("Saga正向操作-跨行转账完成, sagaId={}, paymentId={}", sagaId, paymentId);
            return true;

        } catch (Exception e) {
            log.error("Saga正向操作-跨行转账失败, sagaId={}", sagaId, e);
            accountMapper.unfreezeBalance(fromAccountId, amountFen, LocalDateTime.now());
            order.setStatus(PaymentStatusEnum.FAILED.getCode());
            order.setChannelStatus(ChannelStatusEnum.CHANNEL_FAILED.getCode());
            order.setUpdateTime(LocalDateTime.now());
            paymentOrderMapper.updateById(order);
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean compensate(String sagaId, Map<String, Object> params) {
        log.info("Saga补偿操作-跨行转账回滚, sagaId={}", sagaId);

        String idempotentKey = "saga:crossbank:compensate:" + sagaId;
        RBucket<Boolean> idempotentBucket = redissonClient.getBucket(idempotentKey);
        if (!idempotentBucket.setIfAbsent(true, 24, TimeUnit.HOURS)) {
            log.warn("Saga补偿操作-跨行转账幂等命中, sagaId={}", sagaId);
            return true;
        }

        String contextKey = "saga:crossbank:context:" + sagaId;
        String contextJson = (String) redissonClient.getBucket(contextKey).get();
        if (contextJson == null) {
            log.warn("Saga补偿操作-上下文不存在, 可能正向操作未执行, sagaId={}", sagaId);
            return true;
        }

        Map<String, Object> context = JSON.parseObject(contextJson, Map.class);
        String fromAccountId = (String) context.get("fromAccountId");
        String toAccountId = (String) context.get("toAccountId");
        Long amountFen = Long.valueOf(context.get("amountFen").toString());
        String paymentId = (String) context.get("paymentId");
        String channelCode = (String) context.get("channelCode");

        try {
            PaymentOrder order = paymentOrderMapper.selectByPaymentId(paymentId);
            if (order != null) {
                order.setStatus(PaymentStatusEnum.FAILED.getCode());
                order.setChannelStatus(ChannelStatusEnum.CHANNEL_FAILED.getCode());
                order.setUpdateTime(LocalDateTime.now());
                paymentOrderMapper.updateById(order);
            }

            ChannelAdapter adapter = channelAdapterFactory.getAdapter(channelCode);
            Map<String, Object> cancelParams = new HashMap<>();
            cancelParams.put("paymentId", paymentId);
            cancelParams.put("fromAccountId", fromAccountId);
            cancelParams.put("toAccountId", toAccountId);
            cancelParams.put("amountFen", amountFen);
            adapter.cancelTransaction(cancelParams);

        } catch (Exception e) {
            log.error("Saga补偿操作-渠道取消失败, sagaId={}", sagaId, e);
        }

        int unfreezeUpdated = accountMapper.unfreezeBalance(
                fromAccountId,
                amountFen,
                LocalDateTime.now()
        );

        if (unfreezeUpdated == 0) {
            log.warn("Saga补偿操作-解冻失败, 可能资金已解冻, sagaId={}, accountId={}", sagaId, fromAccountId);
        }

        deleteAccountCache(fromAccountId);

        log.info("Saga补偿操作-跨行转账回滚完成, sagaId={}", sagaId);
        return true;
    }

    private void saveForwardContext(String sagaId, String fromAccountId, String toAccountId,
                                    Long amountFen, String paymentId, String channelCode) {
        Map<String, Object> context = new HashMap<>();
        context.put("fromAccountId", fromAccountId);
        context.put("toAccountId", toAccountId);
        context.put("amountFen", amountFen);
        context.put("paymentId", paymentId);
        context.put("channelCode", channelCode);

        String contextKey = "saga:crossbank:context:" + sagaId;
        redissonClient.getBucket(contextKey).set(JSON.toJSONString(context), 24, TimeUnit.HOURS);
    }

    private void deleteAccountCache(String accountId) {
        String cacheKey = CommonConstants.ACCOUNT_CACHE_PREFIX + accountId;
        redissonClient.getBucket(cacheKey).delete();
    }
}
