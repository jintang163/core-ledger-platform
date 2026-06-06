package com.bank.core.common.utils;

import com.bank.core.common.constants.CommonConstants;
import com.bank.core.common.enums.ResultCodeEnum;
import com.bank.core.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁工具类
 * 
 * 基于Redisson实现的分布式锁，提供以下特性：
 * 1. 可重入锁：支持同一线程重复获取锁
 * 2. 自动续期：Redisson看门狗机制，业务未执行完自动续期
 * 3. 阻塞超时：等待锁超时后抛出异常，避免无限等待
 * 4. 自动释放：finally块中自动释放锁，避免死锁
 * 
 * 使用场景：
 * - 分布式环境下的并发控制
 * - 防止重复提交
 * - 防止并发更新
 * - 定时任务单节点执行
 * 
 * 默认配置：
 * - 等待时间：3秒
 * - 持有时间：30秒
 */
@Slf4j
public class DistributedLockUtil {

    /** Redisson客户端实例，由Spring容器初始化时注入 */
    private static RedissonClient redissonClient;

    /**
     * 设置Redisson客户端
     * 在Spring容器初始化时调用，完成工具类的静态注入
     * @param client Redisson客户端实例
     */
    public static void setRedissonClient(RedissonClient client) {
        redissonClient = client;
    }

    /**
     * 执行带分布式锁的操作（有返回值）
     * 使用默认等待时间和持有时间
     * @param lockKey 锁的Key
     * @param supplier 要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果
     */
    public static <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        return executeWithLock(lockKey, CommonConstants.DEFAULT_LOCK_WAIT_TIME,
                CommonConstants.DEFAULT_LOCK_LEASE_TIME, supplier);
    }

    /**
     * 执行带分布式锁的操作（有返回值，自定义超时时间）
     * 
     * 执行流程：
     * 1. 获取Redisson可重入锁
     * 2. 尝试获取锁，等待指定时间
     * 3. 获取成功后执行业务逻辑
     * 4. finally块中释放锁
     * 
     * @param lockKey 锁的Key
     * @param waitTime 等待锁的最大时间（秒）
     * @param leaseTime 锁的持有时间（秒），超过自动释放
     * @param supplier 要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果
     * @throws BusinessException 获取锁失败或被中断时抛出
     */
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

    /**
     * 执行带分布式锁的操作（无返回值）
     * 使用默认等待时间和持有时间
     * @param lockKey 锁的Key
     * @param runnable 要执行的操作
     */
    public static void executeWithLock(String lockKey, Runnable runnable) {
        executeWithLock(lockKey, CommonConstants.DEFAULT_LOCK_WAIT_TIME,
                CommonConstants.DEFAULT_LOCK_LEASE_TIME, runnable);
    }

    /**
     * 执行带分布式锁的操作（无返回值，自定义超时时间）
     * 
     * @param lockKey 锁的Key
     * @param waitTime 等待锁的最大时间（秒）
     * @param leaseTime 锁的持有时间（秒），超过自动释放
     * @param runnable 要执行的操作
     * @throws BusinessException 获取锁失败或被中断时抛出
     */
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
