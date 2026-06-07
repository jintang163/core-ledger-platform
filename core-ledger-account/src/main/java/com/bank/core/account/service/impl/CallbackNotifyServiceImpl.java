package com.bank.core.account.service.impl;

import com.alibaba.fastjson.JSON;
import com.bank.core.account.entity.CallbackLog;
import com.bank.core.account.mapper.CallbackLogMapper;
import com.bank.core.account.service.CallbackNotifyService;
import com.bank.core.common.constants.CommonConstants;
import com.bank.core.common.utils.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallbackNotifyServiceImpl implements CallbackNotifyService {

    private final CallbackLogMapper callbackLogMapper;
    private final RedissonClient redissonClient;
    private final RestTemplate restTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CallbackLog saveCallback(String callbackType, String callbackUrl, String requestMethod,
                                     Map<String, Object> requestBody, Map<String, String> requestHeaders,
                                     String businessNo, Integer maxRetryTimes) {
        String logId = SnowflakeIdGenerator.nextIdStr();
        log.info("保存回调记录, logId={}, callbackType={}, callbackUrl={}, businessNo={}",
                logId, callbackType, callbackUrl, businessNo);

        CallbackLog callbackLog = new CallbackLog();
        callbackLog.setId(SnowflakeIdGenerator.nextId());
        callbackLog.setLogId(logId);
        callbackLog.setCallbackType(callbackType);
        callbackLog.setCallbackUrl(callbackUrl);
        callbackLog.setRequestMethod(requestMethod != null ? requestMethod : HttpMethod.POST.name());
        callbackLog.setRequestBody(requestBody != null ? JSON.toJSONString(requestBody) : null);
        callbackLog.setRequestHeaders(requestHeaders != null ? JSON.toJSONString(requestHeaders) : null);
        callbackLog.setBusinessNo(businessNo);
        callbackLog.setStatus(CommonConstants.CALLBACK_STATUS_PENDING);
        callbackLog.setRetryCount(0);
        callbackLog.setMaxRetryTimes(maxRetryTimes != null ? maxRetryTimes : CommonConstants.MAX_RETRY_TIMES);
        callbackLog.setNextRetryTime(System.currentTimeMillis());
        callbackLog.setCreateTime(LocalDateTime.now());
        callbackLog.setUpdateTime(LocalDateTime.now());
        callbackLog.setDeleted(0);
        callbackLogMapper.insert(callbackLog);

        log.info("回调记录保存成功, logId={}", logId);
        return callbackLog;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean executeCallback(String logId) {
        log.info("执行HTTP回调, logId={}", logId);

        String lockKey = "callback:notify:lock:" + logId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                log.warn("执行回调获取锁失败, logId={}", logId);
                return false;
            }

            CallbackLog callbackLog = callbackLogMapper.selectByLogId(logId);
            if (callbackLog == null) {
                log.error("回调记录不存在, logId={}", logId);
                return false;
            }

            if (CommonConstants.CALLBACK_STATUS_SUCCESS.equals(callbackLog.getStatus())) {
                log.warn("回调已成功, logId={}", logId);
                return true;
            }

            HttpMethod method = HttpMethod.resolve(callbackLog.getRequestMethod());
            if (method == null) {
                method = HttpMethod.POST;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (callbackLog.getRequestHeaders() != null) {
                try {
                    Map<String, String> headerMap = JSON.parseObject(callbackLog.getRequestHeaders(), Map.class);
                    if (headerMap != null) {
                        headerMap.forEach(headers::set);
                    }
                } catch (Exception e) {
                    log.warn("解析请求头失败, logId={}", logId, e);
                }
            }

            HttpEntity<String> requestEntity = new HttpEntity<>(callbackLog.getRequestBody(), headers);

            log.info("发送HTTP回调, logId={}, method={}, url={}, body={}",
                    logId, method, callbackLog.getCallbackUrl(), callbackLog.getRequestBody());

            ResponseEntity<String> response = restTemplate.exchange(
                    callbackLog.getCallbackUrl(),
                    method,
                    requestEntity,
                    String.class
            );

            String responseBody = response.getBody();
            Integer responseStatus = response.getStatusCodeValue();

            boolean success = response.getStatusCode().is2xxSuccessful() && isCallbackSuccess(responseBody);

            if (success) {
                callbackLogMapper.markAsSuccess(
                        callbackLog.getId(),
                        responseBody,
                        responseStatus,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );
                log.info("HTTP回调成功, logId={}, status={}", logId, responseStatus);
                return true;
            } else {
                Long nextRetryTime = calculateNextRetryTime(callbackLog.getRetryCount() + 1);
                String errorMsg = "回调失败, HTTP状态: " + responseStatus + ", 响应: " + responseBody;
                callbackLogMapper.updateStatus(
                        callbackLog.getId(),
                        CommonConstants.CALLBACK_STATUS_FAILED,
                        nextRetryTime,
                        errorMsg,
                        responseBody,
                        responseStatus,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                );
                log.warn("HTTP回调失败, logId={}, status={}, error={}", logId, responseStatus, errorMsg);
                return false;
            }
        } catch (Exception e) {
            log.error("HTTP回调异常, logId={}", logId, e);

            CallbackLog callbackLog = callbackLogMapper.selectByLogId(logId);
            if (callbackLog != null) {
                Long nextRetryTime = calculateNextRetryTime(callbackLog.getRetryCount() + 1);
                callbackLogMapper.updateStatus(
                        callbackLog.getId(),
                        CommonConstants.CALLBACK_STATUS_FAILED,
                        nextRetryTime,
                        e.getMessage(),
                        null,
                        null,
                        LocalDateTime.now(),
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
    @Scheduled(fixedDelay = 60000)
    public void retryFailedCallbacks() {
        String lockKey = "callback:notify:retry:lock";
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(0, 60, TimeUnit.SECONDS)) {
                return;
            }

            List<CallbackLog> pendingCallbacks = callbackLogMapper.selectPendingCallbacks(
                    System.currentTimeMillis(),
                    100
            );

            log.info("扫描待重试回调, 数量={}", pendingCallbacks.size());

            for (CallbackLog callbackLog : pendingCallbacks) {
                try {
                    executeCallback(callbackLog.getLogId());
                } catch (Exception e) {
                    log.error("重试回调失败, logId={}", callbackLog.getLogId(), e);
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
    public CallbackLog getByLogId(String logId) {
        return callbackLogMapper.selectByLogId(logId);
    }

    private boolean isCallbackSuccess(String responseBody) {
        if (responseBody == null) {
            return false;
        }
        try {
            Map<String, Object> responseMap = JSON.parseObject(responseBody, Map.class);
            if (responseMap != null) {
                Object code = responseMap.get("code");
                Object success = responseMap.get("success");
                if (success != null) {
                    return Boolean.TRUE.equals(success);
                }
                if (code != null) {
                    return "200".equals(code.toString()) || "0".equals(code.toString());
                }
            }
        } catch (Exception e) {
            log.debug("解析回调响应失败, 默认为成功, body={}", responseBody);
        }
        return true;
    }

    private Long calculateNextRetryTime(int retryCount) {
        long delay = (long) Math.pow(2, Math.min(retryCount, 5)) * 60 * 1000;
        return System.currentTimeMillis() + delay;
    }
}
