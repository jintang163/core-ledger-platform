package com.bank.core.common.utils;

import com.bank.core.common.constants.CommonConstants;
import com.bank.core.common.enums.ResultCodeEnum;
import com.bank.core.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

/**
 * 重试工具类
 * 支持带指数退避的自动重试机制
 *
 * 指数退避策略：
 * - 初始延迟：100ms
 * - 每次重试延迟 = 初始延迟 * (退避乘数 ^ 重试次数)
 * - 最大延迟：5000ms
 *
 * 示例：第0次重试等待100ms，第1次等待200ms，第2次等待400ms，以此类推
 */
@Slf4j
public class RetryUtil {

    private RetryUtil() {
    }

    /**
     * 使用默认参数执行带重试的操作
     * @param callable 需要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果
     */
    public static <T> T executeWithRetry(Callable<T> callable) {
        return executeWithRetry(callable,
                CommonConstants.OPTIMISTIC_LOCK_MAX_RETRY,
                CommonConstants.RETRY_INITIAL_DELAY,
                CommonConstants.RETRY_MAX_DELAY,
                CommonConstants.RETRY_BACKOFF_MULTIPLIER);
    }

    /**
     * 执行带重试和指数退避的操作
     * @param callable 需要执行的操作
     * @param maxRetries 最大重试次数
     * @param initialDelayMs 初始延迟（毫秒）
     * @param maxDelayMs 最大延迟（毫秒）
     * @param backoffMultiplier 退避乘数
     * @param <T> 返回值类型
     * @return 操作结果
     */
    public static <T> T executeWithRetry(Callable<T> callable,
                                          int maxRetries,
                                          long initialDelayMs,
                                          long maxDelayMs,
                                          double backoffMultiplier) {
        int retryCount = 0;
        long delay = initialDelayMs;

        while (true) {
            try {
                return callable.call();
            } catch (BusinessException e) {
                if (ResultCodeEnum.CONCURRENT_UPDATE_FAILED.equals(e.getCode()) && retryCount < maxRetries) {
                    retryCount++;
                    log.warn("乐观锁冲突，第{}次重试，延迟{}ms", retryCount, delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BusinessException(ResultCodeEnum.DISTRIBUTED_LOCK_FAILED, "重试被中断");
                    }
                    delay = Math.min((long) (delay * backoffMultiplier), maxDelayMs);
                } else {
                    throw e;
                }
            } catch (Exception e) {
                log.error("重试执行异常", e);
                if (retryCount < maxRetries) {
                    retryCount++;
                    log.warn("操作异常，第{}次重试，延迟{}ms", retryCount, delay);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BusinessException(ResultCodeEnum.DISTRIBUTED_LOCK_FAILED, "重试被中断");
                    }
                    delay = Math.min((long) (delay * backoffMultiplier), maxDelayMs);
                } else {
                    throw new BusinessException(ResultCodeEnum.TRANSACTION_FAILED, "操作失败，已重试" + retryCount + "次");
                }
            }
        }
    }

    /**
     * 执行带重试的Runnable操作（无返回值）
     * @param runnable 需要执行的操作
     */
    public static void executeWithRetry(Runnable runnable) {
        executeWithRetry(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 计算指数退避延迟时间
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
        long delay = initialDelayMs * (long) Math.pow(backoffMultiplier, retryCount - 1);
        return Math.min(delay, maxDelayMs);
    }
}
