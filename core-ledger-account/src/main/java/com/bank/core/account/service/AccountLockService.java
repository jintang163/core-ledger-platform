package com.bank.core.account.service;

import java.util.List;
import java.util.function.Supplier;

/**
 * 账户级分布式锁服务接口
 * 提供细粒度的账户级别分布式锁，防止同一账户并发操作冲突
 */
public interface AccountLockService {

    /**
     * 对单个账户加锁并执行操作
     * @param accountId 账户ID
     * @param supplier 要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果
     */
    <T> T executeWithAccountLock(String accountId, Supplier<T> supplier);

    /**
     * 对单个账户加锁并执行操作（自定义超时时间）
     * @param accountId 账户ID
     * @param waitTime 等待锁时间（秒）
     * @param leaseTime 持有锁时间（秒）
     * @param supplier 要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果
     */
    <T> T executeWithAccountLock(String accountId, long waitTime, long leaseTime, Supplier<T> supplier);

    /**
     * 对单个账户加锁并执行无返回值操作
     * @param accountId 账户ID
     * @param runnable 要执行的操作
     */
    void executeWithAccountLock(String accountId, Runnable runnable);

    /**
     * 对多个账户加锁并执行操作（按账户ID排序防止死锁）
     * 自动按账户ID排序获取锁，避免死锁问题
     * @param accountIds 账户ID列表
     * @param supplier 要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果
     */
    <T> T executeWithMultiAccountLock(List<String> accountIds, Supplier<T> supplier);

    /**
     * 对多个账户加锁并执行无返回值操作（按账户ID排序防止死锁）
     * @param accountIds 账户ID列表
     * @param runnable 要执行的操作
     */
    void executeWithMultiAccountLock(List<String> accountIds, Runnable runnable);

    /**
     * 尝试获取账户锁（非阻塞）
     * @param accountId 账户ID
     * @return 是否获取成功
     */
    boolean tryLock(String accountId);

    /**
     * 释放账户锁
     * @param accountId 账户ID
     */
    void unlock(String accountId);

    /**
     * 检查账户是否被锁定
     * @param accountId 账户ID
     * @return 是否被锁定
     */
    boolean isLocked(String accountId);
}
