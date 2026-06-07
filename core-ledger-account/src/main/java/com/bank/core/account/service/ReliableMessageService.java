package com.bank.core.account.service;

import com.bank.core.account.entity.ReliableMessage;

import java.util.Map;

public interface ReliableMessageService {

    ReliableMessage saveMessage(String messageType, String topic, String tag,
                                 String messageKey, Object messageContent,
                                 String businessNo, Integer maxRetryTimes);

    boolean sendMessage(String messageId);

    boolean confirmSendSuccess(String messageId);

    void retryFailedMessages();

    ReliableMessage getByMessageId(String messageId);
}
