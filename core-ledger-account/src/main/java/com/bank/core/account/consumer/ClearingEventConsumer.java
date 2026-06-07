package com.bank.core.account.consumer;

import com.bank.core.api.event.ClearingEvent;
import com.bank.core.common.constants.CommonConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = CommonConstants.ROCKETMQ_TOPIC_TRANSACTION,
        consumerGroup = "clearing-event-consumer-group",
        selectorExpression = "clearing:result"
)
public class ClearingEventConsumer implements RocketMQListener<ClearingEvent> {

    @Override
    public void onMessage(ClearingEvent event) {
        log.info("接收清算事件, eventId: {}, eventType: {}, clearingId: {}, status: {}",
                event.getEventId(), event.getEventType(), event.getClearingId(), event.getStatus());

        try {
            handleClearingResult(event);
            log.info("处理清算事件成功, eventId: {}", event.getEventId());
        } catch (Exception e) {
            log.error("处理清算事件失败, eventId: {}", event.getEventId(), e);
            throw e;
        }
    }

    private void handleClearingResult(ClearingEvent event) {
        log.info("清算结果处理 - clearingId: {}, transactionId: {}, accountId: {}, amount: {}, status: {}",
                event.getClearingId(), event.getTransactionId(), event.getAccountId(),
                event.getAmount(), event.getStatus());

        if ("SUCCESS".equals(event.getStatus())) {
            handleClearingSuccess(event);
        } else if ("FAILED".equals(event.getStatus())) {
            handleClearingFailed(event);
        } else {
            log.warn("未知清算状态: {}", event.getStatus());
        }
    }

    private void handleClearingSuccess(ClearingEvent event) {
        log.info("清算成功处理 - 更新账户状态, accountId: {}, amount: {}",
                event.getAccountId(), event.getAmount());

        sendAccountNotification(event.getAccountId(),
                "清算已完成，金额 " + event.getAmount() + " " + event.getCurrency());
    }

    private void handleClearingFailed(ClearingEvent event) {
        log.error("清算失败处理 - 触发异常流程, clearingId: {}, error: {}",
                event.getClearingId(), event.getRemark());

        sendAccountNotification(event.getAccountId(),
                "清算处理失败，请联系客服，清算ID: " + event.getClearingId());
    }

    private void sendAccountNotification(String accountId, String message) {
        log.info("发送账户通知, accountId: {}, message: {}", accountId, message);
    }
}
