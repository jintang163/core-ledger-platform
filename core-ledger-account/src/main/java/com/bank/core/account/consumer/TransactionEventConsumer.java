package com.bank.core.account.consumer;

import com.bank.core.api.event.ClearingEvent;
import com.bank.core.api.event.TransactionEvent;
import com.bank.core.common.constants.CommonConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = CommonConstants.ROCKETMQ_TOPIC_TRANSACTION,
        consumerGroup = "transaction-event-consumer-group",
        selectorExpression = "transfer:success || payment:success || refund:success || clearing:result"
)
public class TransactionEventConsumer implements RocketMQListener<TransactionEvent> {

    @Override
    public void onMessage(TransactionEvent event) {
        log.info("接收交易事件, eventId: {}, eventType: {}, transactionId: {}",
                event.getEventId(), event.getEventType(), event.getTransactionId());

        try {
            switch (event.getEventType()) {
                case CommonConstants.ROCKETMQ_TAG_TRANSFER_SUCCESS:
                    handleTransferSuccess(event);
                    break;
                case CommonConstants.ROCKETMQ_TAG_PAYMENT_SUCCESS:
                    handlePaymentSuccess(event);
                    break;
                case CommonConstants.ROCKETMQ_TAG_REFUND_SUCCESS:
                    handleRefundSuccess(event);
                    break;
                default:
                    log.warn("未知交易事件类型: {}", event.getEventType());
            }
            log.info("处理交易事件成功, eventId: {}", event.getEventId());
        } catch (Exception e) {
            log.error("处理交易事件失败, eventId: {}", event.getEventId(), e);
            throw e;
        }
    }

    private void handleTransferSuccess(TransactionEvent event) {
        log.info("转账成功事件处理 - 发送通知, transferId: {}, fromAccountId: {}, toAccountId: {}",
                event.getTransactionId(), event.getFromAccountId(), event.getToAccountId());

        sendAccountNotification(event.getFromAccountId(), "您的账户已转出 " + event.getAmount() + " " + event.getCurrency());
        sendAccountNotification(event.getToAccountId(), "您的账户已转入 " + event.getAmount() + " " + event.getCurrency());
    }

    private void handlePaymentSuccess(TransactionEvent event) {
        log.info("支付成功事件处理 - 发送通知, paymentId: {}, payerAccountId: {}, payeeAccountId: {}",
                event.getTransactionId(), event.getFromAccountId(), event.getToAccountId());

        sendAccountNotification(event.getFromAccountId(), "您的账户已支付 " + event.getAmount() + " " + event.getCurrency());
    }

    private void handleRefundSuccess(TransactionEvent event) {
        log.info("退款成功事件处理 - 发送通知, refundPaymentId: {}, originalPaymentId: {}",
                event.getTransactionId(), event.getRemark());

        sendAccountNotification(event.getToAccountId(), "您的账户已收到退款 " + event.getAmount() + " " + event.getCurrency());
    }

    private void sendAccountNotification(String accountId, String message) {
        log.info("发送账户通知, accountId: {}, message: {}", accountId, message);
    }
}
