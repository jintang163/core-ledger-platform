package com.bank.core.account.service;

import com.bank.core.account.entity.CallbackLog;

import java.util.Map;

public interface CallbackNotifyService {

    CallbackLog saveCallback(String callbackType, String callbackUrl, String requestMethod,
                              Map<String, Object> requestBody, Map<String, String> requestHeaders,
                              String businessNo, Integer maxRetryTimes);

    boolean executeCallback(String logId);

    void retryFailedCallbacks();

    CallbackLog getByLogId(String logId);
}
