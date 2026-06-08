package com.bank.core.common.utils;

import com.bank.core.common.constants.CommonConstants;
import com.bank.core.common.enums.ResultCodeEnum;
import com.bank.core.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

@Slf4j
public class IdempotentUtil {

    private static RedissonClient redissonClient;

    public static void setRedissonClient(RedissonClient client) {
        redissonClient = client;
    }

    public static void checkIdempotent(String requestId) {
        checkIdempotent(requestId, CommonConstants.IDEMPOTENT_DEFAULT_TTL_HOURS, TimeUnit.HOURS);
    }

    public static void checkIdempotent(String requestId, long ttl, TimeUnit timeUnit) {
        checkIdempotent(CommonConstants.ACCOUNT_IDEMPOTENT_PREFIX, requestId, ttl, timeUnit);
    }

    public static void checkIdempotent(String prefix, String businessNo, long ttl, TimeUnit timeUnit) {
        if (businessNo == null || businessNo.trim().isEmpty()) {
            log.warn("业务流水号不能为空");
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "业务流水号不能为空");
        }
        String key = prefix + businessNo;
        RBucket<String> bucket = redissonClient.getBucket(key);
        boolean existed = bucket.setIfAbsent("PROCESSING", ttl, timeUnit);
        if (!existed) {
            log.warn("重复请求, prefix: {}, businessNo: {}", prefix, businessNo);
            throw new BusinessException(ResultCodeEnum.DUPLICATE_REQUEST);
        }
    }

    public static String getIdempotentValue(String prefix, String businessNo) {
        if (businessNo == null || businessNo.trim().isEmpty()) {
            return null;
        }
        String key = prefix + businessNo;
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    public static void setIdempotentResult(String prefix, String businessNo, String result, long ttl, TimeUnit timeUnit) {
        if (businessNo == null || businessNo.trim().isEmpty()) {
            return;
        }
        String key = prefix + businessNo;
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.set(result, ttl, timeUnit);
        log.debug("设置幂等结果, prefix: {}, businessNo: {}, result: {}", prefix, businessNo, result);
    }

    public static void removeIdempotent(String requestId) {
        removeIdempotent(CommonConstants.ACCOUNT_IDEMPOTENT_PREFIX, requestId);
    }

    public static void removeIdempotent(String prefix, String businessNo) {
        if (businessNo == null || businessNo.isEmpty()) {
            return;
        }
        String key = prefix + businessNo;
        redissonClient.getBucket(key).delete();
        log.debug("删除幂等键, prefix: {}, businessNo: {}", prefix, businessNo);
    }
}
