package com.bank.core.account.service.impl;

import com.alibaba.fastjson.JSON;
import com.bank.core.account.entity.ReliableMessage;
import com.bank.core.account.mapper.ReliableMessageMapper;
import com.bank.core.account.service.ReliableMessageService;
import com.bank.core.common.constants.CommonConstants;
import com.bank.core.common.utils.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReliableMessageServiceImpl implements ReliableMessageService {

    private final ReliableMessageMapper reliableMessageMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final RedissonClient redissonClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReliableMessage saveMessage(String messageType, String topic, String tag,
                                        String messageKey, Object messageContent,
                                        String businessNo, Integer maxRetryTimes) {
        String messageId = SnowflakeIdGenerator.nextIdStr();
        log.info("保存可靠消息, messageId={}, messageType={}, topic={}, tag={}, businessNo={}",
                messageId, messageType, topic, tag, businessNo);

        ReliableMessage message = new ReliableMessage();
        message.setId(SnowflakeIdGenerator.nextId());
        message.setMessageId(messageId);
        message.setMessageType(messageType);
        message.setTopic(topic);
        message.setTag(tag);
        message.setMessageKey(messageKey);
        message.setMessageContent(JSON.toJSONString(messageContent));
        message.setBusinessNo(businessNo);
        message.setStatus(CommonConstants.MESSAGE_STATUS_PENDING);
        message.setRetryCount(0);
        message.setMaxRetryTimes(maxRetryTimes != null ? maxRetryTimes : CommonConstants.MAX_RETRY_TIMES);
        message.setNextRetryTime(System.currentTimeMillis());
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        message.setDeleted(0);
        reliableMessageMapper.insert(message);

        log.info("可靠消息保存成功, messageId={}", messageId);
        return message;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean sendMessage(String messageId) {
        log.info("发送可靠消息, messageId={}", messageId);

        String lockKey = "reliable:message:send:lock:" + messageId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                log.warn("发送消息获取锁失败, messageId={}", messageId);
                return false;
            }

            ReliableMessage message = reliableMessageMapper.selectByMessageId(messageId);
            if (message == null) {
                log.error("消息不存在, messageId={}", messageId);
                return false;
            }

            if (CommonConstants.MESSAGE_STATUS_SUCCESS.equals(message.getStatus())) {
                log.warn("消息已发送成功, messageId={}", messageId);
                return true;
            }

            message.setStatus(CommonConstants.MESSAGE_STATUS_SENDING);
            message.setUpdateTime(LocalDateTime.now());
            reliableMessageMapper.updateById(message);

            String destination = message.getTopic() + ":" + message.getTag();
            Object payload = JSON.parse(message.getMessageContent());
            rocketMQTemplate.send(destination, MessageBuilder.withPayload(payload)
                    .setHeader("KEYS", message.getMessageKey() != null ? message.getMessageKey() : message.getMessageId())
                    .build());

            confirmSendSuccess(messageId);

            log.info("可靠消息发送成功, messageId={}, destination={}", messageId, destination);
            return true;
        } catch (Exception e) {
            log.error("可靠消息发送失败, messageId={}", messageId, e);

            ReliableMessage message = reliableMessageMapper.selectByMessageId(messageId);
            if (message != null) {
                Long nextRetryTime = calculateNextRetryTime(message.getRetryCount() + 1);
                reliableMessageMapper.updateStatus(
                        message.getId(),
                        CommonConstants.MESSAGE_STATUS_FAILED,
                        nextRetryTime,
                        e.getMessage(),
                        LocalDateTime.now()
                );
            }
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmSendSuccess(String messageId) {
        ReliableMessage message = reliableMessageMapper.selectByMessageId(messageId);
        if (message == null) {
            return false;
        }
        reliableMessageMapper.markAsSuccess(
                message.getId(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        return true;
    }

    @Override
    @Scheduled(fixedDelay = 30000)
    public void retryFailedMessages() {
        String lockKey = "reliable:message:retry:lock";
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(0, 60, TimeUnit.SECONDS)) {
                return;
            }

            List<ReliableMessage> pendingMessages = reliableMessageMapper.selectPendingMessages(
                    System.currentTimeMillis(),
                    100
            );

            log.info("扫描待重试消息, 数量={}", pendingMessages.size());

            for (ReliableMessage message : pendingMessages) {
                try {
                    sendMessage(message.getMessageId());
                } catch (Exception e) {
                    log.error("重试消息失败, messageId={}", message.getMessageId(), e);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public ReliableMessage getByMessageId(String messageId) {
        return reliableMessageMapper.selectByMessageId(messageId);
    }

    private Long calculateNextRetryTime(int retryCount) {
        long delay = (long) Math.pow(2, Math.min(retryCount, 5)) * 60 * 1000;
        return System.currentTimeMillis() + delay;
    }
}
