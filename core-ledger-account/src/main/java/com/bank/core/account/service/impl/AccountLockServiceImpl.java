package com.bank.core.account.service.impl;

import com.bank.core.account.service.AccountLockService;
import com.bank.core.common.constants.CommonConstants;
import com.bank.core.common.enums.ResultCodeEnum;
import com.bank.core.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 账户级分布式锁服务实现类
 *
 * 核心功能：
 * 1. 细粒度锁：每个账户独立的分布式锁，避免粗粒度锁的性能瓶颈
 * 2. 死锁预防：多账户锁时按ID排序获取，避免死锁
 * 3. 锁自动释放：使用Redisson的看门狗机制自动续期
 * 4. 可重入锁：支持同一线程重入
 *
 * 设计要点：
 * - 使用Redisson的可重入锁RLock
 * - 锁前缀：account:balance:lock:
 * - 默认等待时间1秒，持有时间5秒
 * - 多账户锁时按字典序排序获取，避免A->B和B->A的死锁
 * - 锁粒度越细，并发性越高，但管理越复杂
 *
 * 适用场景：
 * - 账户余额更新
 * - 账户冻结/解冻
 * - 账户销户
 * - 任何需要保证账户操作原子性的场景
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountLockServiceImpl implements AccountLockService {

    private final RedissonClient redissonClient;

    @Override
    public <T> T executeWithAccountLock(String accountId, Supplier<T> supplier) {
        // 委托给带超时参数的方法，使用默认配置
        return executeWithAccountLock(accountId,
                CommonConstants.ACCOUNT_LOCK_WAIT_TIME,
                CommonConstants.ACCOUNT_LOCK_LEASE_TIME,
                supplier);
    }

    @Override
    public <T> T executeWithAccountLock(String accountId, long waitTime, long leaseTime, Supplier<T> supplier) {
        // 构造锁key，每个账户独立的锁，实现细粒度的并发控制
        String lockKey = CommonConstants.ACCOUNT_BALANCE_LOCK_PREFIX + accountId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;

        try {
            // 尝试获取锁，设置等待时间和持有时间
            // waitTime：获取锁的最大等待时间
            // leaseTime：锁的自动释放时间（看门狗机制会自动续期）
            locked = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!locked) {
                // 获取锁失败，抛出业务异常
                log.warn("获取账户锁失败, accountId: {}, lockKey: {}", accountId, lockKey);
                throw new BusinessException(ResultCodeEnum.DISTRIBUTED_LOCK_FAILED,
                        "账户操作繁忙，请稍后重试, accountId: " + accountId);
            }

            log.debug("获取账户锁成功, accountId: {}, lockKey: {}", accountId, lockKey);
            // 执行业务逻辑
            return supplier.get();

        } catch (InterruptedException e) {
            // 线程被中断，重置中断标志位
            Thread.currentThread().interrupt();
            log.error("获取账户锁被中断, accountId: {}, lockKey: {}", accountId, lockKey, e);
            throw new BusinessException(ResultCodeEnum.DISTRIBUTED_LOCK_FAILED, "账户操作被中断");
        } finally {
            // 只有当前线程持有锁时才释放，避免释放其他线程的锁
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("释放账户锁成功, accountId: {}, lockKey: {}", accountId, lockKey);
            }
        }
    }

    @Override
    public void executeWithAccountLock(String accountId, Runnable runnable) {
        // 适配Runnable接口，委托给Supplier版本
        executeWithAccountLock(accountId, () -> {
            runnable.run();
            return null;
        });
    }

    @Override
    public <T> T executeWithMultiAccountLock(List<String> accountIds, Supplier<T> supplier) {
        // 空列表直接执行业务逻辑，不需要加锁
        if (accountIds == null || accountIds.isEmpty()) {
            return supplier.get();
        }

        // 关键：对账户ID进行排序，按固定顺序获取锁
        // 这是避免死锁的核心：无论账户ID传入顺序如何，都按字典序获取锁
        // 例如：A->B 和 B->A 都会先锁A再锁B，避免循环等待
        List<String> sortedAccountIds = new ArrayList<>(accountIds);
        Collections.sort(sortedAccountIds);

        List<RLock> locks = new ArrayList<>();
        boolean allLocked = true;

        try {
            // 按排序后的顺序逐个获取锁
            for (String accountId : sortedAccountIds) {
                String lockKey = CommonConstants.ACCOUNT_BALANCE_LOCK_PREFIX + accountId;
                RLock lock = redissonClient.getLock(lockKey);

                boolean locked = lock.tryLock(
                        CommonConstants.ACCOUNT_LOCK_WAIT_TIME,
                        CommonConstants.ACCOUNT_LOCK_LEASE_TIME,
                        TimeUnit.SECONDS);

                if (!locked) {
                    // 只要有一个锁获取失败，标记为未全部锁定，跳出循环
                    allLocked = false;
                    log.warn("获取账户锁失败, accountId: {}, lockKey: {}", accountId, lockKey);
                    break;
                }

                // 记录已获取的锁，便于后续释放
                locks.add(lock);
                log.debug("获取账户锁成功, accountId: {}, lockKey: {}", accountId, lockKey);
            }

            // 有任何一个锁获取失败，都抛出异常
            if (!allLocked) {
                throw new BusinessException(ResultCodeEnum.DISTRIBUTED_LOCK_FAILED,
                        "账户操作繁忙，请稍后重试");
            }

            // 所有锁都获取成功，执行业务逻辑
            return supplier.get();

        } catch (InterruptedException e) {
            // 线程被中断，重置中断标志位
            Thread.currentThread().interrupt();
            log.error("获取账户锁被中断", e);
            throw new BusinessException(ResultCodeEnum.DISTRIBUTED_LOCK_FAILED, "账户操作被中断");
        } finally {
            // 按获取锁的逆序释放锁（反向解锁）
            // 从最后一个锁开始释放，避免锁重入问题
            for (int i = locks.size() - 1; i >= 0; i--) {
                RLock lock = locks.get(i);
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.debug("释放账户锁成功, lockKey: {}", lock.getName());
                }
            }
        }
    }

    @Override
    public void executeWithMultiAccountLock(List<String> accountIds, Runnable runnable) {
        // 适配Runnable接口，委托给Supplier版本
        executeWithMultiAccountLock(accountIds, () -> {
            runnable.run();
            return null;
        });
    }

    @Override
    public boolean tryLock(String accountId) {
        // 非阻塞尝试获取锁，等待时间为0，立即返回结果
        String lockKey = CommonConstants.ACCOUNT_BALANCE_LOCK_PREFIX + accountId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            return lock.tryLock(0, CommonConstants.ACCOUNT_LOCK_LEASE_TIME, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // 线程被中断，重置中断标志位，返回false
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void unlock(String accountId) {
        // 手动释放锁
        // 注意：只有当前线程持有锁时才释放，避免释放其他线程的锁
        String lockKey = CommonConstants.ACCOUNT_BALANCE_LOCK_PREFIX + accountId;
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public boolean isLocked(String accountId) {
        // 检查锁是否被任何线程持有
        // 注意：这只是一个瞬时状态，检查完成后锁状态可能已经改变
        String lockKey = CommonConstants.ACCOUNT_BALANCE_LOCK_PREFIX + accountId;
        RLock lock = redissonClient.getLock(lockKey);
        return lock.isLocked();
    }
}
