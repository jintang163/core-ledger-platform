package com.bank.core.common.utils;

import com.bank.core.common.constants.CommonConstants;
import com.bank.core.common.enums.ResultCodeEnum;
import com.bank.core.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RateLimitUtil {

    private static final ConcurrentHashMap<String, Boolean> RATE_LIMIT_INITIALIZED = new ConcurrentHashMap<>();

    private static RedissonClient redissonClient;

    public static void setRedissonClient(RedissonClient client) {
        redissonClient = client;
    }

    public static void checkRateLimit(String accountId, String operationType) {
        checkRateLimit(accountId, operationType,
                CommonConstants.DEFAULT_RATE_LIMIT,
                CommonConstants.DEFAULT_RATE_LIMIT_WINDOW);
    }

    public static void checkRateLimit(String accountId, String operationType,
                                      int limit, int windowSeconds) {
        if (accountId == null || accountId.trim().isEmpty()) {
            log.warn("账户ID不能为空，跳过限流检查");
            return;
        }

        String rateLimitKey = CommonConstants.ACCOUNT_RATE_LIMIT_PREFIX
                + accountId + operationType;

        try {
            RRateLimiter rateLimiter = redissonClient.getRateLimiter(rateLimitKey);

            String initKey = rateLimitKey + ":" + limit + ":" + windowSeconds;
            if (!RATE_LIMIT_INITIALIZED.containsKey(initKey)) {
                rateLimiter.trySetRate(RateType.OVERALL, limit, windowSeconds, RateIntervalUnit.SECONDS);
                RATE_LIMIT_INITIALIZED.put(initKey, Boolean.TRUE);
                log.debug("限流规则初始化, key: {}, limit: {}/{}s", rateLimitKey, limit, windowSeconds);
            }

            boolean acquired = rateLimiter.tryAcquire(1);
            if (!acquired) {
                log.warn("账户操作触发限流, accountId: {}, operationType: {}, limit: {}/{}s",
                        accountId, operationType, limit, windowSeconds);
                throw new BusinessException(ResultCodeEnum.RATE_LIMIT_EXCEEDED);
            }

            log.debug("限流检查通过, accountId: {}, operationType: {}", accountId, operationType);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("限流检查异常，拒绝请求, accountId: {}, operationType: {}", accountId, operationType, e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "系统繁忙，请稍后重试");
        }
    }
}
