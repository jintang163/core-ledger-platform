package com.bank.core.account.service.impl;

import com.bank.core.account.entity.ReliableMessage;
import com.bank.core.account.service.CallbackNotifyService;
import com.bank.core.account.service.MessageProducerService;
import com.bank.core.account.service.ReliableMessageService;
import com.bank.core.api.event.AccountEvent;
import com.bank.core.api.event.ClearingEvent;
import com.bank.core.api.event.TransactionEvent;
import com.bank.core.api.vo.PaymentOrderVO;
import com.bank.core.api.vo.TransferOrderVO;
import com.bank.core.common.constants.CommonConstants;
import com.bank.core.common.utils.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProducerServiceImpl implements MessageProducerService {

    private final ReliableMessageService reliableMessageService;
    private final CallbackNotifyService callbackNotifyService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendAccountChangeMessage(String accountId, String accountNo, String transactionId,
                                          Integer transactionType, BigDecimal amount, String currency,
                                          String requestId, String operator, String businessNo) {
        log.info("发送账户变动消息, accountId={}, transactionId={}, amount={}",
                accountId, transactionId, amount);

        AccountEvent event = new AccountEvent();
        event.setEventId(SnowflakeIdGenerator.nextIdStr());
        event.setEventType(CommonConstants.ROCKETMQ_TAG_ACCOUNT_CHANGE);
        event.setAccountId(accountId);
        event.setAccountNo(accountNo);
        event.setTransactionId(transactionId);
        event.setTransactionType(transactionType);
        event.setBalance(amount);
        event.setCurrency(currency);
        event.setRequestId(requestId);
        event.setOperator(operator);
        event.setEventTime(LocalDateTime.now());

        String messageKey = accountId + ":" + transactionId;
        ReliableMessage message = reliableMessageService.saveMessage(
                "ACCOUNT_CHANGE",
                CommonConstants.ROCKETMQ_TOPIC_ACCOUNT,
                CommonConstants.ROCKETMQ_TAG_ACCOUNT_CHANGE,
                messageKey,
                event,
                businessNo,
                null
        );

        reliableMessageService.sendMessage(message.getMessageId());

        log.info("账户变动消息发送完成, messageId={}", message.getMessageId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendTransferSuccessMessage(TransferOrderVO order) {
        log.info("发送转账成功消息, transferId={}, businessNo={}", order.getTransferId(), order.getBusinessNo());

        TransactionEvent event = new TransactionEvent();
        event.setEventId(SnowflakeIdGenerator.nextIdStr());
        event.setEventType(CommonConstants.ROCKETMQ_TAG_TRANSFER_SUCCESS);
        event.setTransactionId(order.getTransferId());
        event.setBusinessNo(order.getBusinessNo());
        event.setFromAccountId(order.getFromAccountId());
        event.setFromAccountNo(order.getFromAccountNo());
        event.setToAccountId(order.getToAccountId());
        event.setToAccountNo(order.getToAccountNo());
        event.setAmount(order.getAmount());
        event.setCurrency(order.getCurrency());
        event.setTransactionType(order.getPaymentType());
        event.setStatus(order.getStatusDesc());
        event.setRemark(order.getRemark());
        event.setOperator(order.getOperator());
        event.setRequestId(order.getRequestId());
        event.setEventTime(LocalDateTime.now());

        String messageKey = order.getTransferId();
        ReliableMessage message = reliableMessageService.saveMessage(
                "TRANSFER_SUCCESS",
                CommonConstants.ROCKETMQ_TOPIC_TRANSACTION,
                CommonConstants.ROCKETMQ_TAG_TRANSFER_SUCCESS,
                messageKey,
                event,
                order.getBusinessNo(),
                null
        );

        reliableMessageService.sendMessage(message.getMessageId());

        log.info("转账成功消息发送完成, messageId={}", message.getMessageId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendPaymentSuccessMessage(PaymentOrderVO order) {
        log.info("发送支付成功消息, paymentId={}, businessNo={}", order.getPaymentId(), order.getBusinessNo());

        TransactionEvent event = new TransactionEvent();
        event.setEventId(SnowflakeIdGenerator.nextIdStr());
        event.setEventType(CommonConstants.ROCKETMQ_TAG_PAYMENT_SUCCESS);
        event.setTransactionId(order.getPaymentId());
        event.setBusinessNo(order.getBusinessNo());
        event.setFromAccountId(order.getPayerAccountId());
        event.setToAccountId(order.getPayeeAccountId());
        event.setAmount(order.getAmount());
        event.setCurrency(order.getCurrency());
        event.setTransactionType(order.getPaymentType());
        event.setStatus(order.getStatusDesc());
        event.setChannelCode(order.getChannelCode());
        event.setRemark(order.getRemark());
        event.setOperator(order.getOperator());
        event.setRequestId(order.getRequestId());
        event.setEventTime(LocalDateTime.now());

        String messageKey = order.getPaymentId();
        ReliableMessage message = reliableMessageService.saveMessage(
                "PAYMENT_SUCCESS",
                CommonConstants.ROCKETMQ_TOPIC_TRANSACTION,
                CommonConstants.ROCKETMQ_TAG_PAYMENT_SUCCESS,
                messageKey,
                event,
                order.getBusinessNo(),
                null
        );

        reliableMessageService.sendMessage(message.getMessageId());

        log.info("支付成功消息发送完成, messageId={}", message.getMessageId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendRefundSuccessMessage(PaymentOrderVO order) {
        log.info("发送退款成功消息, refundPaymentId={}, originalPaymentId={}",
                order.getPaymentId(), order.getOriginalPaymentId());

        TransactionEvent event = new TransactionEvent();
        event.setEventId(SnowflakeIdGenerator.nextIdStr());
        event.setEventType(CommonConstants.ROCKETMQ_TAG_REFUND_SUCCESS);
        event.setTransactionId(order.getPaymentId());
        event.setBusinessNo(order.getBusinessNo());
        event.setFromAccountId(order.getPayerAccountId());
        event.setToAccountId(order.getPayeeAccountId());
        event.setAmount(order.getAmount());
        event.setCurrency(order.getCurrency());
        event.setTransactionType(order.getPaymentType());
        event.setStatus(order.getStatusDesc());
        event.setChannelCode(order.getChannelCode());
        event.setRemark(order.getRemark());
        event.setOperator(order.getOperator());
        event.setRequestId(order.getRequestId());
        event.setEventTime(LocalDateTime.now());

        String messageKey = order.getPaymentId();
        ReliableMessage message = reliableMessageService.saveMessage(
                "REFUND_SUCCESS",
                CommonConstants.ROCKETMQ_TOPIC_TRANSACTION,
                CommonConstants.ROCKETMQ_TAG_REFUND_SUCCESS,
                messageKey,
                event,
                order.getBusinessNo(),
                null
        );

        reliableMessageService.sendMessage(message.getMessageId());

        log.info("退款成功消息发送完成, messageId={}", message.getMessageId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendClearingResultMessage(String clearingId, String transactionId,
                                           String accountId, BigDecimal amount, String currency,
                                           String status, String businessNo) {
        log.info("发送清算结果消息, clearingId={}, transactionId={}, status={}",
                clearingId, transactionId, status);

        ClearingEvent event = new ClearingEvent();
        event.setEventId(SnowflakeIdGenerator.nextIdStr());
        event.setEventType(CommonConstants.ROCKETMQ_TAG_CLEARING_RESULT);
        event.setClearingId(clearingId);
        event.setTransactionId(transactionId);
        event.setAccountId(accountId);
        event.setAmount(amount);
        event.setCurrency(currency);
        event.setStatus(status);
        event.setBusinessNo(businessNo);
        event.setEventTime(LocalDateTime.now());

        String messageKey = clearingId;
        ReliableMessage message = reliableMessageService.saveMessage(
                "CLEARING_RESULT",
                CommonConstants.ROCKETMQ_TOPIC_TRANSACTION,
                CommonConstants.ROCKETMQ_TAG_CLEARING_RESULT,
                messageKey,
                event,
                businessNo,
                null
        );

        reliableMessageService.sendMessage(message.getMessageId());

        log.info("清算结果消息发送完成, messageId={}", message.getMessageId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendCallbackNotify(String callbackType, String callbackUrl,
                                    Map<String, Object> callbackData, String businessNo) {
        log.info("发送回调通知, callbackType={}, callbackUrl={}, businessNo={}",
                callbackType, callbackUrl, businessNo);

        callbackNotifyService.saveCallback(
                callbackType,
                callbackUrl,
                "POST",
                callbackData,
                null,
                businessNo,
                null
        );

        log.info("回调通知保存完成");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendTransferSuccessWithCallback(TransferOrderVO order,
                                                 String callbackUrl, Map<String, Object> additionalData) {
        log.info("发送转账成功消息及回调, transferId={}, callbackUrl={}", order.getTransferId(), callbackUrl);

        sendTransferSuccessMessage(order);

        if (callbackUrl != null && !callbackUrl.trim().isEmpty()) {
            Map<String, Object> callbackData = new HashMap<>();
            callbackData.put("transferId", order.getTransferId());
            callbackData.put("businessNo", order.getBusinessNo());
            callbackData.put("fromAccountId", order.getFromAccountId());
            callbackData.put("toAccountId", order.getToAccountId());
            callbackData.put("amount", order.getAmount());
            callbackData.put("currency", order.getCurrency());
            callbackData.put("status", order.getStatus());
            callbackData.put("statusDesc", order.getStatusDesc());
            callbackData.put("transferTime", order.getTransferTime());
            callbackData.put("remark", order.getRemark());

            if (additionalData != null) {
                callbackData.putAll(additionalData);
            }

            sendCallbackNotify("TRANSFER_SUCCESS", callbackUrl, callbackData, order.getBusinessNo());
        }

        log.info("转账成功消息及回调发送完成");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendPaymentSuccessWithCallback(PaymentOrderVO order,
                                                String callbackUrl, Map<String, Object> additionalData) {
        log.info("发送支付成功消息及回调, paymentId={}, callbackUrl={}", order.getPaymentId(), callbackUrl);

        sendPaymentSuccessMessage(order);

        if (callbackUrl != null && !callbackUrl.trim().isEmpty()) {
            Map<String, Object> callbackData = new HashMap<>();
            callbackData.put("paymentId", order.getPaymentId());
            callbackData.put("businessNo", order.getBusinessNo());
            callbackData.put("payerAccountId", order.getPayerAccountId());
            callbackData.put("payeeAccountId", order.getPayeeAccountId());
            callbackData.put("amount", order.getAmount());
            callbackData.put("currency", order.getCurrency());
            callbackData.put("paymentType", order.getPaymentType());
            callbackData.put("paymentTypeDesc", order.getPaymentTypeDesc());
            callbackData.put("status", order.getStatus());
            callbackData.put("statusDesc", order.getStatusDesc());
            callbackData.put("paymentTime", order.getPaymentTime());
            callbackData.put("channelCode", order.getChannelCode());
            callbackData.put("remark", order.getRemark());
            callbackData.put("sagaId", order.getSagaId());
            callbackData.put("originalPaymentId", order.getOriginalPaymentId());

            if (additionalData != null) {
                callbackData.putAll(additionalData);
            }

            sendCallbackNotify("PAYMENT_SUCCESS", callbackUrl, callbackData, order.getBusinessNo());
        }

        log.info("支付成功消息及回调发送完成");
    }
}
