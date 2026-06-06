package com.bank.core.common.utils;

import com.bank.core.common.constants.CommonConstants;
import com.bank.core.common.enums.ResultCodeEnum;
import com.bank.core.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 重试工具类
 * 
 * 提供带指数退避的自动重试机制，用于处理并发更新冲突等可重试的操作。
 * 
 * 核心特性：
 * 1. 指数退避：等待时间随重试次数指数增长，避免并发冲突加剧
 * 2. 最大重试次数：防止无限重试
 * 3. 区分异常类型：仅对并发更新失败异常进行重试，其他异常直接抛出
 * 4. 随机抖动：添加随机因子避免惊群效应（Thundering Herd）
 * 
 * 默认参数（从CommonConstants读取）：
 * - 最大重试次数：5次
 * - 初始延迟：100ms
 * - 最大延迟：5000ms
 * - 退避乘数：2.0
 * 
 * 指数退避示例（默认参数）：
 * 第1次重试：100ms × 2^0 = 100ms
 * 第2次重试：100ms × 2^1 = 200ms
 * 第3次重试：100ms × 2^2 = 400ms
 * 第4次重试：100ms × 2^3 = 800ms
 * 第5次重试：100ms × 2^4 = 1600ms
 * 
 * 使用场景：
 * - 乐观锁更新失败重试
 * - 网络请求超时重试
 * - 分布式锁竞争重试
 * - 数据库死锁重试
 */
@Slf4j
public class RetryUtil {

    /** 随机抖动因子（延迟的±10%），避免惊群效应 */
    private static final double JITTER_FACTOR = 0.1;

    /**
     * 私有构造函数，禁止实例化工具类
     */
    private RetryUtil() {
    }

    /**
     * 使用默认参数执行带重试的操作（有返回值）
     * 
     * 从CommonConstants读取默认配置：
     * - 最大重试次数：OPTIMISTIC_LOCK_MAX_RETRY
     * - 初始延迟：RETRY_INITIAL_DELAY
     * - 最大延迟：RETRY_MAX_DELAY
     * - 退避乘数：RETRY_BACKOFF_MULTIPLIER
     * 
     * @param callable 需要执行的操作，返回操作结果
     * @param <T> 返回值类型
     * @return 操作结果
     * @throws BusinessException 非可重试异常或超过最大重试次数时抛出
     */
    public static <T> T executeWithRetry(Callable<T> callable) {
        return executeWithRetry(callable,
                CommonConstants.OPTIMISTIC_LOCK_MAX_RETRY,
                CommonConstants.RETRY_INITIAL_DELAY,
                CommonConstants.RETRY_MAX_DELAY,
                CommonConstants.RETRY_BACKOFF_MULTIPLIER);
    }

    /**
     * 执行带重试和指数退避的操作（有返回值，自定义参数）
     * 
     * 完整执行流程：
     * 1. 循环执行业务操作
     * 2. 捕获BusinessException，判断是否为可重试的并发更新异常
     *    - 关键修复：使用getCode()获取枚举值后比较，而非直接比较枚举对象与Integer
     * 3. 可重试且未超过最大次数：计算退避延迟 → 休眠 → 重试
     * 4. 不可重试或超过次数：抛出异常
     * 5. 捕获其他Exception：同样重试（兼容未封装为BusinessException的异常）
     * 
     * @param callable 需要执行的操作，返回操作结果
     * @param maxRetries 最大重试次数
     * @param initialDelayMs 初始延迟（毫秒）
     * @param maxDelayMs 最大延迟（毫秒）
     * @param backoffMultiplier 退避乘数
     * @param <T> 返回值类型
     * @return 操作结果
     * @throws BusinessException 非可重试异常或超过最大重试次数时抛出
     */
    public static <T> T executeWithRetry(Callable<T> callable,
                                          int maxRetries,
                                          long initialDelayMs,
                                          long maxDelayMs,
                                          double backoffMultiplier) {
        // 已重试次数，从0开始计数
        int retryCount = 0;
        // 当前延迟时间，初始为初始延迟
        long delay = initialDelayMs;

        // 无限循环，直到成功或抛出异常
        while (true) {
            try {
                // 1. 执行业务操作，成功则直接返回结果
                return callable.call();
            } catch (BusinessException e) {
                // ============================================================
                // 2. 捕获业务异常，判断是否为可重试的并发更新异常
                // 
                // 【关键BUG修复】
                // 原错误代码：ResultCodeEnum.CONCURRENT_UPDATE_FAILED.equals(e.getCode())
                // 错误原因：ResultCodeEnum.CONCURRENT_UPDATE_FAILED 是枚举对象
                //         e.getCode() 是 Integer 类型（值为3005）
                //         不同类型的对象equals比较永远返回 false，导致重试永远不触发！
                // 
                // 修复后代码：ResultCodeEnum.CONCURRENT_UPDATE_FAILED.getCode().equals(e.getCode())
                // 正确逻辑：先通过getCode()获取枚举的Integer值（3005），再与异常code比较
                // ============================================================
                boolean isRetryable = ResultCodeEnum.CONCURRENT_UPDATE_FAILED.getCode().equals(e.getCode());
                
                if (isRetryable && retryCount < maxRetries) {
                    // 2.1 可重试且未超过最大次数：执行重试
                    retryCount++;
                    
                    // 添加随机抖动，避免惊群效应
                    long delayWithJitter = addJitter(delay);
                    
                    log.warn("乐观锁冲突，第{}次重试，延迟{}ms（含抖动），错误码：{}，错误信息：{}", 
                            retryCount, delayWithJitter, e.getCode(), e.getMessage());
                    
                    try {
                        // 休眠等待，给其他线程完成操作的机会
                        Thread.sleep(delayWithJitter);
                    } catch (InterruptedException ie) {
                        // 线程被中断，恢复中断状态并抛出异常
                        Thread.currentThread().interrupt();
                        throw new BusinessException(ResultCodeEnum.DISTRIBUTED_LOCK_FAILED, "重试被中断");
                    }
                    
                    // 计算下一次的延迟时间（指数增长）
                    delay = Math.min((long) (delay * backoffMultiplier), maxDelayMs);
                } else {
                    // 2.2 不可重试或超过次数：直接抛出异常
                    if (!isRetryable) {
                        log.debug("非可重试异常，直接抛出，错误码：{}，错误信息：{}", e.getCode(), e.getMessage());
                    } else {
                        log.warn("乐观锁重试次数已达上限，最大重试次数：{}，最后错误：{}", 
                                maxRetries, e.getMessage());
                    }
                    throw e;
                }
            } catch (Exception e) {
                // 3. 捕获其他未封装为BusinessException的异常，同样进行重试
                log.error("重试执行异常", e);
                
                if (retryCount < maxRetries) {
                    // 3.1 未超过最大次数：执行重试
                    retryCount++;
                    
                    // 添加随机抖动
                    long delayWithJitter = addJitter(delay);
                    
                    log.warn("操作异常，第{}次重试，延迟{}ms（含抖动）", retryCount, delayWithJitter);
                    
                    try {
                        Thread.sleep(delayWithJitter);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BusinessException(ResultCodeEnum.DISTRIBUTED_LOCK_FAILED, "重试被中断");
                    }
                    
                    delay = Math.min((long) (delay * backoffMultiplier), maxDelayMs);
                } else {
                    // 3.2 超过最大次数：封装为业务异常抛出
                    log.warn("操作重试次数已达上限，最大重试次数：{}", maxRetries);
                    throw new BusinessException(ResultCodeEnum.TRANSACTION_FAILED, 
                            "操作失败，已重试" + retryCount + "次");
                }
            }
        }
    }

    /**
     * 执行带重试的Runnable操作（无返回值，使用默认参数）
     * 
     * 适配Runnable接口，内部通过Callable包装实现
     * 
     * @param runnable 需要执行的操作，无返回值
     */
    public static void executeWithRetry(Runnable runnable) {
        executeWithRetry(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 计算指数退避延迟时间
     * 
     * 计算公式：delay = min(initialDelay × (backoffMultiplier ^ (retryCount-1)), maxDelay)
     * 
     * @param retryCount 当前重试次数（从1开始）
     * @param initialDelayMs 初始延迟（毫秒）
     * @param maxDelayMs 最大延迟（毫秒）
     * @param backoffMultiplier 退避乘数
     * @return 延迟时间（毫秒）
     */
    public static long calculateExponentialDelay(int retryCount,
                                                  long initialDelayMs,
                                                  long maxDelayMs,
                                                  double backoffMultiplier) {
        // 计算基础延迟：initialDelay × (backoffMultiplier ^ (retryCount-1))
        // retryCount从1开始，所以指数是retryCount-1
        long delay = initialDelayMs * (long) Math.pow(backoffMultiplier, retryCount - 1);
        // 限制最大延迟，避免等待时间过长
        return Math.min(delay, maxDelayMs);
    }

    /**
     * 为延迟时间添加随机抖动
     * 
     * 抖动目的：避免多个请求在同一时间点重试，导致新的并发冲突（惊群效应）
     * 抖动范围：[-JITTER_FACTOR × delay, +JITTER_FACTOR × delay]
     * 例如：延迟100ms，抖动因子0.1，则实际延迟在90ms-110ms之间
     * 
     * @param delay 原始延迟时间（毫秒）
     * @return 添加抖动后的延迟时间
     */
    private static long addJitter(long delay) {
        // 计算抖动量：延迟 × 抖动因子 × (-1到1之间的随机数)
        double jitter = delay * JITTER_FACTOR * (ThreadLocalRandom.current().nextDouble() * 2 - 1);
        // 返回添加抖动后的延迟，确保不小于0
        return (long) Math.max(0, delay + jitter);
    }
}
