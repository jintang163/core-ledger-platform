package com.bank.core.common.utils;

import com.bank.core.common.constants.CommonConstants;
import com.bank.core.common.enums.ResultCodeEnum;
import com.bank.core.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class DistributedLockUtil {

    private static RedissonClient redissonClient;

    public static void setRedissonClient(RedissonClient client) {
        redissonClient = client;
    }

    public static <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        return executeWithLock(lockKey, CommonConstants.DEFAULT_LOCK_WAIT_TIME,
                CommonConstants.DEFAULT_LOCK_LEASE_TIME, supplier);
    }

    public static <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("获取分布式锁失败, lockKey: {}", lockKey);
                throw new BusinessException(ResultCodeEnum.DISTRIBUTED_LOCK_FAILED);
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断, lockKey: {}", lockKey, e);
            throw new BusinessException(ResultCodeEnum.DISTRIBUTED_LOCK_FAILED);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public static void executeWithLock(String lockKey, Runnable runnable) {
        executeWithLock(lockKey, CommonConstants.DEFAULT_LOCK_WAIT_TIME,
                CommonConstants.DEFAULT_LOCK_LEASE_TIME, runnable);
    }

    public static void executeWithLock(String lockKey, long waitTime, long leaseTime, Runnable runnable) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("获取分布式锁失败, lockKey: {}", lockKey);
                throw new BusinessException(ResultCodeEnum.DISTRIBUTED_LOCK_FAILED);
            }
            runnable.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断, lockKey: {}", lockKey, e);
            throw new BusinessException(ResultCodeEnum.DISTRIBUTED_LOCK_FAILED);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
