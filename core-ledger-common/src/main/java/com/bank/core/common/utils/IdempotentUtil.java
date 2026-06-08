package com.bank.core.common.utils;

import com.bank.core.common.constants.CommonConstants;
import com.bank.core.common.enums.ResultCodeEnum;
import com.bank.core.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class IdempotentUtil {

    public static final String PROCESSING = "PROCESSING";

    private static RedissonClient redissonClient;

    public static void setRedissonClient(RedissonClient client) {
        redissonClient = client;
    }

    public static <T> T executeWithIdempotent(String prefix, String businessNo,
                                               Class<T> resultType, Supplier<T> action) {
        return executeWithIdempotent(prefix, businessNo,
                CommonConstants.IDEMPOTENT_DEFAULT_TTL_HOURS, TimeUnit.HOURS,
                resultType, action);
    }

    public static <T> T executeWithIdempotent(String prefix, String businessNo,
                                               long ttl, TimeUnit timeUnit,
                                               Class<T> resultType, Supplier<T> action) {
        T result = checkAndGetResult(prefix, businessNo, resultType);
        if (result != null) {
            return result;
        }

        acquireLock(prefix, businessNo, ttl, timeUnit);

        try {
            result = checkAndGetResult(prefix, businessNo, resultType);
            if (result != null) {
                return result;
            }

            T executeResult = action.get();

            setIdempotentResult(prefix, businessNo,
                    executeResult != null ? executeResult.toString() : "SUCCESS",
                    ttl, timeUnit);

            return executeResult;
        } catch (Exception e) {
            if (e instanceof BusinessException
                    && ResultCodeEnum.DUPLICATE_REQUEST.getCode().equals(((BusinessException) e).getCode())) {
                throw e;
            }
            removeIdempotent(prefix, businessNo);
            throw e;
        }
    }

    public static <T> T checkAndGetResult(String prefix, String businessNo, Class<T> resultType) {
        String value = getIdempotentValue(prefix, businessNo);
        if (value == null) {
            return null;
        }
        if (PROCESSING.equals(value)) {
            log.warn("请求处理中, prefix: {}, businessNo: {}", prefix, businessNo);
            throw new BusinessException(ResultCodeEnum.DUPLICATE_REQUEST, "请求处理中，请稍后重试");
        }
        if (resultType == String.class) {
            return resultType.cast(value);
        }
        return null;
    }

    public static void acquireLock(String prefix, String businessNo, long ttl, TimeUnit timeUnit) {
        if (businessNo == null || businessNo.trim().isEmpty()) {
            log.warn("业务流水号不能为空");
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "业务流水号不能为空");
        }
        String key = prefix + businessNo;
        RBucket<String> bucket = redissonClient.getBucket(key);
        boolean acquired = bucket.setIfAbsent(PROCESSING, ttl, timeUnit);
        if (!acquired) {
            log.warn("重复请求或请求处理中, prefix: {}, businessNo: {}", prefix, businessNo);
            String currentValue = bucket.get();
            if (PROCESSING.equals(currentValue)) {
                throw new BusinessException(ResultCodeEnum.DUPLICATE_REQUEST, "请求处理中，请稍后重试");
            }
            throw new BusinessException(ResultCodeEnum.DUPLICATE_REQUEST);
        }
    }

    @Deprecated
    public static void checkIdempotent(String requestId) {
        acquireLock(CommonConstants.ACCOUNT_IDEMPOTENT_PREFIX, requestId,
                CommonConstants.IDEMPOTENT_DEFAULT_TTL_HOURS, TimeUnit.HOURS);
    }

    @Deprecated
    public static void checkIdempotent(String requestId, long ttl, TimeUnit timeUnit) {
        acquireLock(CommonConstants.ACCOUNT_IDEMPOTENT_PREFIX, requestId, ttl, timeUnit);
    }

    @Deprecated
    public static void checkIdempotent(String prefix, String businessNo, long ttl, TimeUnit timeUnit) {
        acquireLock(prefix, businessNo, ttl, timeUnit);
    }

    public static String getIdempotentValue(String prefix, String businessNo) {
        if (businessNo == null || businessNo.trim().isEmpty()) {
            return null;
        }
        String key = prefix + businessNo;
        try {
            RBucket<String> bucket = redissonClient.getBucket(key);
            return bucket.get();
        } catch (Exception e) {
            log.error("获取幂等值异常, prefix: {}, businessNo: {}", prefix, businessNo, e);
            return null;
        }
    }

    public static void setIdempotentResult(String prefix, String businessNo, String result, long ttl, TimeUnit timeUnit) {
        if (businessNo == null || businessNo.trim().isEmpty()) {
            return;
        }
        String key = prefix + businessNo;
        try {
            RBucket<String> bucket = redissonClient.getBucket(key);
            bucket.set(result, ttl, timeUnit);
            log.debug("设置幂等结果, prefix: {}, businessNo: {}, result: {}", prefix, businessNo, result);
        } catch (Exception e) {
            log.error("设置幂等结果异常, prefix: {}, businessNo: {}", prefix, businessNo, e);
        }
    }

    public static void removeIdempotent(String requestId) {
        removeIdempotent(CommonConstants.ACCOUNT_IDEMPOTENT_PREFIX, requestId);
    }

    public static void removeIdempotent(String prefix, String businessNo) {
        if (businessNo == null || businessNo.isEmpty()) {
            return;
        }
        String key = prefix + businessNo;
        try {
            redissonClient.getBucket(key).delete();
            log.debug("删除幂等键, prefix: {}, businessNo: {}", prefix, businessNo);
        } catch (Exception e) {
            log.error("删除幂等键异常, prefix: {}, businessNo: {}", prefix, businessNo, e);
        }
    }

    public static void deleteAccountCache(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return;
        }
        try {
            String cacheKey = CommonConstants.ACCOUNT_CACHE_PREFIX + accountId;
            redissonClient.getBucket(cacheKey).delete();

            String balanceCacheKey = CommonConstants.ACCOUNT_BALANCE_CACHE_PREFIX + accountId;
            redissonClient.getBucket(balanceCacheKey).delete();

            String hotCacheKey = CommonConstants.HOT_ACCOUNT_LOCK_PREFIX + accountId;
            redissonClient.getBucket(hotCacheKey).delete();

            String shardCacheKey = CommonConstants.SHARD_ACCOUNT_CACHE_PREFIX + accountId;
            redissonClient.getBucket(shardCacheKey).delete();

            log.debug("删除账户所有缓存, accountId: {}", accountId);
        } catch (Exception e) {
            log.error("删除账户缓存异常, accountId: {}", accountId, e);
        }
    }
}
