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
        checkIdempotent(requestId, 24, TimeUnit.HOURS);
    }

    public static void checkIdempotent(String requestId, long ttl, TimeUnit timeUnit) {
        if (requestId == null || requestId.trim().isEmpty()) {
            log.warn("请求ID不能为空");
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "请求ID不能为空");
        }
        String key = CommonConstants.ACCOUNT_IDEMPOTENT_PREFIX + requestId;
        RBucket<String> bucket = redissonClient.getBucket(key);
        boolean existed = bucket.setIfAbsent("1", ttl, timeUnit);
        if (!existed) {
            log.warn("重复请求, requestId: {}", requestId);
            throw new BusinessException(ResultCodeEnum.DUPLICATE_REQUEST);
        }
    }

    public static void removeIdempotent(String requestId) {
        if (requestId == null || requestId.isEmpty()) {
            return;
        }
        String key = CommonConstants.ACCOUNT_IDEMPOTENT_PREFIX + requestId;
        redissonClient.getBucket(key).delete();
    }
}
