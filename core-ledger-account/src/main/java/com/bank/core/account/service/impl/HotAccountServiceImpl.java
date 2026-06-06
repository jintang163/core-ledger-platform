package com.bank.core.account.service.impl;

import com.alibaba.fastjson.JSON;
import com.bank.core.account.entity.Account;
import com.bank.core.account.entity.AccountShard;
import com.bank.core.account.mapper.AccountMapper;
import com.bank.core.account.mapper.AccountShardMapper;
import com.bank.core.account.service.HotAccountService;
import com.bank.core.common.constants.CommonConstants;
import com.bank.core.common.enums.AccountStatusEnum;
import com.bank.core.common.enums.HotAccountStatusEnum;
import com.bank.core.common.enums.ResultCodeEnum;
import com.bank.core.common.enums.ShardingStrategyEnum;
import com.bank.core.common.exception.BusinessException;
import com.bank.core.common.utils.DistributedLockUtil;
import com.bank.core.common.utils.RetryUtil;
import com.bank.core.common.utils.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 热点账户服务实现类
 *
 * 核心功能：
 * 1. 热点账户分片：将热点账户拆分为多个影子子账户，分散并发压力
 * 2. 智能路由：根据策略（随机/轮询/哈希）选择合适的影子账户
 * 3. 定期归并：定时将影子账户余额归并到主账户
 * 4. 余额查询：自动聚合主账户和所有影子账户的余额
 *
 * 设计要点：
 * - 影子账户与主账户同币种、同类型
 * - 扣款时路由到影子账户，避免主账户并发冲突
 * - 低峰期（默认凌晨2点）自动归并所有影子账户余额到主账户
 * - 使用分布式锁防止并发归并
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotAccountServiceImpl implements HotAccountService {

    private final AccountMapper accountMapper;
    private final AccountShardMapper accountShardMapper;
    private final RedissonClient redissonClient;

    /** 轮询计数器 */
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);

    /** 随机数生成器 */
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public boolean isHotAccount(String accountId) {
        String cacheKey = CommonConstants.HOT_ACCOUNT_LOCK_PREFIX + accountId;
        RBucket<String> cacheBucket = redissonClient.getBucket(cacheKey);
        String cached = cacheBucket.get();
        if (cached != null) {
            return HotAccountStatusEnum.HOT_SHARDING.getCode().toString().equals(cached)
                    || HotAccountStatusEnum.HOT_BUFFER.getCode().toString().equals(cached);
        }

        Account account = accountMapper.selectByAccountId(accountId);
        if (account == null) {
            return false;
        }

        Integer hotStatus = account.getHotStatus();
        boolean isHot = HotAccountStatusEnum.HOT_SHARDING.getCode().equals(hotStatus)
                || HotAccountStatusEnum.HOT_BUFFER.getCode().equals(hotStatus);

        cacheBucket.set(isHot ? hotStatus.toString() : "0", 5, TimeUnit.MINUTES);
        return isHot;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AccountShard> createShards(String mainAccountId, Integer shardCount, Integer shardingStrategy) {
        log.info("创建热点账户分片, mainAccountId: {}, shardCount: {}", mainAccountId, shardCount);

        Account mainAccount = accountMapper.selectByAccountId(mainAccountId);
        if (mainAccount == null) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST);
        }

        if (shardCount == null) {
            shardCount = CommonConstants.DEFAULT_SHARD_COUNT;
        }
        if (shardCount < CommonConstants.MIN_SHARD_COUNT) {
            shardCount = CommonConstants.MIN_SHARD_COUNT;
        }
        if (shardCount > CommonConstants.MAX_SHARD_COUNT) {
            shardCount = CommonConstants.MAX_SHARD_COUNT;
        }

        if (shardingStrategy == null) {
            shardingStrategy = ShardingStrategyEnum.RANDOM.getCode();
        }

        for (int i = 0; i < shardCount; i++) {
            String shardId = mainAccountId + CommonConstants.SHARD_ID_SUFFIX + String.format("%02d", i);

            AccountShard existing = accountShardMapper.selectByShardId(shardId);
            if (existing != null) {
                continue;
            }

            AccountShard shard = new AccountShard();
            shard.setId(SnowflakeIdGenerator.nextId());
            shard.setShardId(shardId);
            shard.setMainAccountId(mainAccountId);
            shard.setShardIndex(i);
            shard.setShardAccountNo(SnowflakeIdGenerator.generateAccountNo());
            shard.setCurrency(mainAccount.getCurrency());
            shard.setBalance(0L);
            shard.setStatus(0);
            shard.setMergeStatus(0);
            shard.setCreateTime(LocalDateTime.now());
            shard.setUpdateTime(LocalDateTime.now());
            shard.setDeleted(0);

            accountShardMapper.insert(shard);
        }

        mainAccount.setHotStatus(HotAccountStatusEnum.HOT_SHARDING.getCode());
        mainAccount.setShardCount(shardCount);
        mainAccount.setShardingStrategy(shardingStrategy);
        mainAccount.setUpdateTime(LocalDateTime.now());
        accountMapper.updateById(mainAccount);

        String cacheKey = CommonConstants.HOT_ACCOUNT_LOCK_PREFIX + mainAccountId;
        redissonClient.getBucket(cacheKey).delete();

        log.info("热点账户分片创建完成, mainAccountId: {}, shardCount: {}", mainAccountId, shardCount);
        return accountShardMapper.selectActiveShards(mainAccountId);
    }

    @Override
    public AccountShard routeShard(String mainAccountId) {
        List<AccountShard> shards = getActiveShardsFromCache(mainAccountId);
        if (shards == null || shards.isEmpty()) {
            shards = accountShardMapper.selectActiveShards(mainAccountId);
            if (shards.isEmpty()) {
                throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST, "热点账户没有可用分片: " + mainAccountId);
            }
            String cacheKey = CommonConstants.SHARD_ACCOUNT_CACHE_PREFIX + mainAccountId;
            redissonClient.getBucket(cacheKey).set(JSON.toJSONString(shards), 10, TimeUnit.MINUTES);
        }

        Account mainAccount = accountMapper.selectByAccountId(mainAccountId);
        if (mainAccount == null || mainAccount.getShardingStrategy() == null) {
            return selectRandomShard(shards);
        }

        ShardingStrategyEnum strategy = ShardingStrategyEnum.getByCode(mainAccount.getShardingStrategy());
        if (strategy == null) {
            strategy = ShardingStrategyEnum.RANDOM;
        }

        return switch (strategy) {
            case ROUND_ROBIN -> selectRoundRobinShard(shards);
            case HASH -> selectHashShard(shards, Thread.currentThread().getName());
            default -> selectRandomShard(shards);
        };
    }

    @Override
    public boolean updateShardBalance(String shardId, Long amountFen) {
        log.debug("更新影子账户余额, shardId: {}, amount: {}分", shardId, amountFen);

        try {
            RetryUtil.executeWithRetry(() -> {
                AccountShard shard = accountShardMapper.selectByShardId(shardId);
                if (shard == null) {
                    throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST, "影子账户不存在: " + shardId);
                }

                int updated = accountShardMapper.updateBalanceWithVersion(
                        shardId, amountFen, shard.getVersion(), LocalDateTime.now());

                if (updated == 0) {
                    throw new BusinessException(ResultCodeEnum.CONCURRENT_UPDATE_FAILED);
                }
                return null;
            });
            return true;
        } catch (Exception e) {
            log.error("更新影子账户余额失败, shardId: {}", shardId, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long mergeShards(String mainAccountId) {
        log.info("归并影子账户余额, mainAccountId: {}", mainAccountId);

        String lockKey = CommonConstants.SHARD_MERGE_LOCK_PREFIX + mainAccountId;
        return DistributedLockUtil.executeWithLock(lockKey, () -> {
            Account mainAccount = accountMapper.selectByAccountId(mainAccountId);
            if (mainAccount == null) {
                throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST);
            }

            List<AccountShard> shards = accountShardMapper.selectActiveShards(mainAccountId);
            long totalAmount = 0L;

            for (AccountShard shard : shards) {
                if (shard.getBalance() == null || shard.getBalance() == 0L) {
                    continue;
                }

                totalAmount += shard.getBalance();

                accountShardMapper.updateMergeStatus(shard.getShardId(), 1, LocalDateTime.now());

                RetryUtil.executeWithRetry(() -> {
                    Account freshAccount = accountMapper.selectByAccountId(mainAccountId);
                    int updated = accountMapper.updateBalanceWithVersion(
                            mainAccountId, shard.getBalance(),
                            freshAccount.getVersion(), LocalDateTime.now());
                    if (updated == 0) {
                        throw new BusinessException(ResultCodeEnum.CONCURRENT_UPDATE_FAILED);
                    }
                    return null;
                });

                accountShardMapper.resetBalanceAfterMerge(shard.getShardId(), LocalDateTime.now(), LocalDateTime.now());
            }

            String cacheKey = CommonConstants.SHARD_ACCOUNT_CACHE_PREFIX + mainAccountId;
            redissonClient.getBucket(cacheKey).delete();

            log.info("影子账户归并完成, mainAccountId: {}, 归并金额: {}分", mainAccountId, totalAmount);
            return totalAmount;
        });
    }

    @Override
    @Scheduled(cron = "${account.hot.shard-merge-cron:" + CommonConstants.SHARD_MERGE_CRON + "}")
    public void mergeAllShards() {
        log.info("开始定时归并所有热点账户影子分片");

        String lockKey = "hot:account:merge:all:lock";
        DistributedLockUtil.executeWithLock(lockKey, 1L, 3600L, () -> {
            List<Account> hotAccounts = accountMapper.selectAllHotAccounts();
            if (hotAccounts.isEmpty()) {
                log.info("没有需要归并的热点账户");
                return null;
            }

            log.info("找到{}个热点账户需要归并", hotAccounts.size());

            int successCount = 0;
            int failCount = 0;
            long totalMergedAmount = 0L;

            for (Account account : hotAccounts) {
                try {
                    Long mergedAmount = mergeShards(account.getAccountId());
                    totalMergedAmount += mergedAmount;
                    successCount++;
                    log.info("归并热点账户成功, accountId: {}, 归并金额: {}分",
                            account.getAccountId(), mergedAmount);
                } catch (Exception e) {
                    failCount++;
                    log.error("归并热点账户失败, accountId: {}", account.getAccountId(), e);
                }
            }

            log.info("热点账户影子分片归并完成, 总计: {}, 成功: {}, 失败: {}, 归并总金额: {}分",
                    hotAccounts.size(), successCount, failCount, totalMergedAmount);

            return null;
        });
    }

    @Override
    public Long getTotalAvailableBalance(String mainAccountId) {
        Account mainAccount = accountMapper.selectByAccountId(mainAccountId);
        if (mainAccount == null) {
            return 0L;
        }

        long total = mainAccount.getBalance() != null ? mainAccount.getBalance() : 0L;

        if (isHotAccount(mainAccountId)) {
            List<AccountShard> shards = accountShardMapper.selectActiveShards(mainAccountId);
            for (AccountShard shard : shards) {
                if (shard.getBalance() != null) {
                    total += shard.getBalance();
                }
            }
        }

        return total;
    }

    @Override
    public List<AccountShard> getShards(String mainAccountId) {
        return accountShardMapper.selectByMainAccountId(mainAccountId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsHotAccount(String accountId, Integer shardCount) {
        log.info("标记账户为热点账户, accountId: {}, shardCount: {}", accountId, shardCount);

        Account account = accountMapper.selectByAccountId(accountId);
        if (account == null) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST);
        }

        if (!AccountStatusEnum.NORMAL.getCode().equals(account.getStatus())) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_STATUS_ERROR, "账户状态异常");
        }

        createShards(accountId, shardCount, ShardingStrategyEnum.RANDOM.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unmarkAsHotAccount(String accountId) {
        log.info("取消热点账户标记, accountId: {}", accountId);

        mergeShards(accountId);

        Account account = accountMapper.selectByAccountId(accountId);
        if (account != null) {
            account.setHotStatus(HotAccountStatusEnum.NORMAL.getCode());
            account.setUpdateTime(LocalDateTime.now());
            accountMapper.updateById(account);
        }

        List<AccountShard> shards = accountShardMapper.selectByMainAccountId(accountId);
        for (AccountShard shard : shards) {
            shard.setStatus(1);
            shard.setUpdateTime(LocalDateTime.now());
            accountShardMapper.updateById(shard);
        }

        String cacheKey = CommonConstants.HOT_ACCOUNT_LOCK_PREFIX + accountId;
        redissonClient.getBucket(cacheKey).delete();
        cacheKey = CommonConstants.SHARD_ACCOUNT_CACHE_PREFIX + accountId;
        redissonClient.getBucket(cacheKey).delete();

        log.info("热点账户标记取消完成, accountId: {}", accountId);
    }

    @SuppressWarnings("unchecked")
    private List<AccountShard> getActiveShardsFromCache(String mainAccountId) {
        String cacheKey = CommonConstants.SHARD_ACCOUNT_CACHE_PREFIX + mainAccountId;
        RBucket<String> cacheBucket = redissonClient.getBucket(cacheKey);
        String cacheValue = cacheBucket.get();
        if (cacheValue != null) {
            return JSON.parseArray(cacheValue, AccountShard.class);
        }
        return null;
    }

    private AccountShard selectRandomShard(List<AccountShard> shards) {
        int index = secureRandom.nextInt(shards.size());
        return shards.get(index);
    }

    private AccountShard selectRoundRobinShard(List<AccountShard> shards) {
        int index = roundRobinCounter.getAndIncrement() % shards.size();
        if (index < 0) {
            index = 0;
            roundRobinCounter.set(0);
        }
        return shards.get(index);
    }

    private AccountShard selectHashShard(List<AccountShard> shards, String key) {
        int hash = Math.abs(key.hashCode());
        int index = hash % shards.size();
        return shards.get(index);
    }
}
