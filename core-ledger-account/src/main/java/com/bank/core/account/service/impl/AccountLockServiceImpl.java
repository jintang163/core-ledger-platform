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
        return executeWithAccountLock(accountId,
                CommonConstants.ACCOUNT_LOCK_WAIT_TIME,
                CommonConstants.ACCOUNT_LOCK_LEASE_TIME,
                supplier);
    }

    @Override
    public <T> T executeWithAccountLock(String accountId, long waitTime, long leaseTime, Supplier<T> supplier) {
        String lockKey = CommonConstants.ACCOUNT_BALANCE_LOCK_PREFIX + accountId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("获取账户锁失败, accountId: {}, lockKey: {}", accountId, lockKey);
                throw new BusinessException(ResultCodeEnum.DISTRIBUTED_LOCK_FAILED,
                        "账户操作繁忙，请稍后重试, accountId: " + accountId);
            }

            log.debug("获取账户锁成功, accountId: {}, lockKey: {}", accountId, lockKey);
            return supplier.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取账户锁被中断, accountId: {}, lockKey: {}", accountId, lockKey, e);
            throw new BusinessException(ResultCodeEnum.DISTRIBUTED_LOCK_FAILED, "账户操作被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("释放账户锁成功, accountId: {}, lockKey: {}", accountId, lockKey);
            }
        }
    }

    @Override
    public void executeWithAccountLock(String accountId, Runnable runnable) {
        executeWithAccountLock(accountId, () -> {
            runnable.run();
            return null;
        });
    }

    @Override
    public <T> T executeWithMultiAccountLock(List<String> accountIds, Supplier<T> supplier) {
        if (accountIds == null || accountIds.isEmpty()) {
            return supplier.get();
        }

        List<String> sortedAccountIds = new ArrayList<>(accountIds);
        Collections.sort(sortedAccountIds);

        List<RLock> locks = new ArrayList<>();
        boolean allLocked = true;

        try {
            for (String accountId : sortedAccountIds) {
                String lockKey = CommonConstants.ACCOUNT_BALANCE_LOCK_PREFIX + accountId;
                RLock lock = redissonClient.getLock(lockKey);

                boolean locked = lock.tryLock(
                        CommonConstants.ACCOUNT_LOCK_WAIT_TIME,
                        CommonConstants.ACCOUNT_LOCK_LEASE_TIME,
                        TimeUnit.SECONDS);

                if (!locked) {
                    allLocked = false;
                    log.warn("获取账户锁失败, accountId: {}, lockKey: {}", accountId, lockKey);
                    break;
                }

                locks.add(lock);
                log.debug("获取账户锁成功, accountId: {}, lockKey: {}", accountId, lockKey);
            }

            if (!allLocked) {
                throw new BusinessException(ResultCodeEnum.DISTRIBUTED_LOCK_FAILED,
                        "账户操作繁忙，请稍后重试");
            }

            return supplier.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取账户锁被中断", e);
            throw new BusinessException(ResultCodeEnum.DISTRIBUTED_LOCK_FAILED, "账户操作被中断");
        } finally {
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
        executeWithMultiAccountLock(accountIds, () -> {
            runnable.run();
            return null;
        });
    }

    @Override
    public boolean tryLock(String accountId) {
        String lockKey = CommonConstants.ACCOUNT_BALANCE_LOCK_PREFIX + accountId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            return lock.tryLock(0, CommonConstants.ACCOUNT_LOCK_LEASE_TIME, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void unlock(String accountId) {
        String lockKey = CommonConstants.ACCOUNT_BALANCE_LOCK_PREFIX + accountId;
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public boolean isLocked(String accountId) {
        String lockKey = CommonConstants.ACCOUNT_BALANCE_LOCK_PREFIX + accountId;
        RLock lock = redissonClient.getLock(lockKey);
        return lock.isLocked();
    }
}
