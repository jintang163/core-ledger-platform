package com.bank.core.account.consumer;

import com.bank.core.api.event.AccountEvent;
import com.bank.core.common.constants.CommonConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = CommonConstants.ROCKETMQ_TOPIC_ACCOUNT,
        consumerGroup = "account-event-consumer-group",
        selectorExpression = "create || freeze || unfreeze || close"
)
public class AccountEventConsumer implements RocketMQListener<AccountEvent> {

    @Override
    public void onMessage(AccountEvent event) {
        log.info("接收账户事件, eventId: {}, eventType: {}, accountId: {}",
                event.getEventId(), event.getEventType(), event.getAccountId());

        try {
            switch (event.getEventType()) {
                case CommonConstants.ROCKETMQ_TAG_ACCOUNT_CREATE:
                    handleAccountCreate(event);
                    break;
                case CommonConstants.ROCKETMQ_TAG_ACCOUNT_FREEZE:
                    handleAccountFreeze(event);
                    break;
                case CommonConstants.ROCKETMQ_TAG_ACCOUNT_UNFREEZE:
                    handleAccountUnfreeze(event);
                    break;
                case CommonConstants.ROCKETMQ_TAG_ACCOUNT_CLOSE:
                    handleAccountClose(event);
                    break;
                default:
                    log.warn("未知事件类型: {}", event.getEventType());
            }
            log.info("处理账户事件成功, eventId: {}", event.getEventId());
        } catch (Exception e) {
            log.error("处理账户事件失败, eventId: {}", event.getEventId(), e);
            throw e;
        }
    }

    private void handleAccountCreate(AccountEvent event) {
        log.info("账户创建事件处理 - 发送通知, accountId: {}, userId: {}",
                event.getAccountId(), event.getUserId());
    }

    private void handleAccountFreeze(AccountEvent event) {
        log.info("账户冻结事件处理 - 风险通知, accountId: {}, freezeType: {}",
                event.getAccountId(), event.getFreezeType());
    }

    private void handleAccountUnfreeze(AccountEvent event) {
        log.info("账户解冻事件处理 - 通知用户, accountId: {}", event.getAccountId());
    }

    private void handleAccountClose(AccountEvent event) {
        log.info("账户销户事件处理 - 清理资源, accountId: {}", event.getAccountId());
    }
}
