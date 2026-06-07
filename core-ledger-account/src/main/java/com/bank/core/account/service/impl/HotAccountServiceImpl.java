package com.bank.core.account.service.impl;

import com.alibaba.fastjson.JSON;
import com.bank.core.account.config.PrometheusConfig;
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
    private final PrometheusConfig prometheusConfig;

    /**
     * 轮询计数器
     * 用于轮询路由策略，记录当前轮询位置
     * 使用AtomicInteger保证线程安全
     */
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);

    /**
     * 随机数生成器
     * 用于随机路由策略
     * 使用SecureRandom提供更高的随机性和安全性
     */
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public boolean isHotAccount(String accountId) {
        // 构造缓存key，优先从缓存获取结果，减少数据库查询
        String cacheKey = CommonConstants.HOT_ACCOUNT_LOCK_PREFIX + accountId;
        RBucket<String> cacheBucket = redissonClient.getBucket(cacheKey);
        String cached = cacheBucket.get();
        if (cached != null) {
            // 缓存命中，直接判断是否为热点状态（2-已分片 或 3-已启用缓冲）
            return HotAccountStatusEnum.HOT_SHARDING.getCode().toString().equals(cached)
                    || HotAccountStatusEnum.HOT_BUFFER.getCode().toString().equals(cached);
        }

        // 缓存未命中，查询数据库
        Account account = accountMapper.selectByAccountId(accountId);
        if (account == null) {
            return false;
        }

        // 判断账户是否为热点账户（状态为已分片或已启用缓冲）
        Integer hotStatus = account.getHotStatus();
        boolean isHot = HotAccountStatusEnum.HOT_SHARDING.getCode().equals(hotStatus)
                || HotAccountStatusEnum.HOT_BUFFER.getCode().equals(hotStatus);

        // 写入缓存，有效期5分钟，减少频繁查询
        // 注意：缓存值存实际状态码，普通账户存"0"
        cacheBucket.set(isHot ? hotStatus.toString() : "0", 5, TimeUnit.MINUTES);
        return isHot;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AccountShard> createShards(String mainAccountId, Integer shardCount, Integer shardingStrategy) {
        log.info("创建热点账户分片, mainAccountId: {}, shardCount: {}", mainAccountId, shardCount);

        // 校验主账户是否存在
        Account mainAccount = accountMapper.selectByAccountId(mainAccountId);
        if (mainAccount == null) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST);
        }

        // 分片数量参数校验，确保在合理范围内
        if (shardCount == null) {
            shardCount = CommonConstants.DEFAULT_SHARD_COUNT;
        }
        if (shardCount < CommonConstants.MIN_SHARD_COUNT) {
            shardCount = CommonConstants.MIN_SHARD_COUNT;
        }
        if (shardCount > CommonConstants.MAX_SHARD_COUNT) {
            shardCount = CommonConstants.MAX_SHARD_COUNT;
        }

        // 分片策略默认使用随机路由
        if (shardingStrategy == null) {
            shardingStrategy = ShardingStrategyEnum.RANDOM.getCode();
        }

        // 循环创建分片
        // 分片ID规则：主账户ID + 后缀 + 两位序号（如 ACC1001_SHARD_01）
        for (int i = 0; i < shardCount; i++) {
            String shardId = mainAccountId + CommonConstants.SHARD_ID_SUFFIX + String.format("%02d", i);

            // 幂等处理：分片已存在则跳过，避免重复创建
            AccountShard existing = accountShardMapper.selectByShardId(shardId);
            if (existing != null) {
                continue;
            }

            // 创建影子账户，与主账户同币种
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

        // 更新主账户状态为"已分片热点"，记录分片数量和策略
        mainAccount.setHotStatus(HotAccountStatusEnum.HOT_SHARDING.getCode());
        mainAccount.setShardCount(shardCount);
        mainAccount.setShardingStrategy(shardingStrategy);
        mainAccount.setUpdateTime(LocalDateTime.now());
        accountMapper.updateById(mainAccount);

        // 删除缓存，确保下次查询获取最新状态
        String cacheKey = CommonConstants.HOT_ACCOUNT_LOCK_PREFIX + mainAccountId;
        redissonClient.getBucket(cacheKey).delete();

        log.info("热点账户分片创建完成, mainAccountId: {}, shardCount: {}", mainAccountId, shardCount);
        return accountShardMapper.selectActiveShards(mainAccountId);
    }

    @Override
    public AccountShard routeShard(String mainAccountId) {
        // 优先从缓存获取活跃分片列表，减少数据库查询
        List<AccountShard> shards = getActiveShardsFromCache(mainAccountId);
        if (shards == null || shards.isEmpty()) {
            // 缓存未命中，查询数据库
            shards = accountShardMapper.selectActiveShards(mainAccountId);
            if (shards.isEmpty()) {
                throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST, "热点账户没有可用分片: " + mainAccountId);
            }
            // 写入缓存，有效期10分钟
            String cacheKey = CommonConstants.SHARD_ACCOUNT_CACHE_PREFIX + mainAccountId;
            redissonClient.getBucket(cacheKey).set(JSON.toJSONString(shards), 10, TimeUnit.MINUTES);
        }

        // 查询主账户获取分片策略
        Account mainAccount = accountMapper.selectByAccountId(mainAccountId);
        if (mainAccount == null || mainAccount.getShardingStrategy() == null) {
            // 未配置策略，默认使用随机路由
            return selectRandomShard(shards);
        }

        // 根据配置的策略选择分片
        ShardingStrategyEnum strategy = ShardingStrategyEnum.getByCode(mainAccount.getShardingStrategy());
        if (strategy == null) {
            strategy = ShardingStrategyEnum.RANDOM;
        }

        // 根据策略路由到不同的分片选择算法
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
                    prometheusConfig.recordHotAccountConflict();
                    throw new BusinessException(ResultCodeEnum.CONCURRENT_UPDATE_FAILED);
                }
                return null;
            });
            return true;
        } catch (Exception e) {
            log.error("更新影子账户余额失败, shardId: {}", shardId, e);
            prometheusConfig.recordHotAccountConflict();
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long mergeShards(String mainAccountId) {
        log.info("归并影子账户余额, mainAccountId: {}", mainAccountId);

        final long[] result = {0L};
        prometheusConfig.recordHotAccountMergeLatency(() -> {
            String lockKey = CommonConstants.SHARD_MERGE_LOCK_PREFIX + mainAccountId;
            result[0] = DistributedLockUtil.executeWithLock(lockKey, () -> {
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

                    try {
                        RetryUtil.executeWithRetry(() -> {
                            Account freshAccount = accountMapper.selectByAccountId(mainAccountId);
                            int updated = accountMapper.updateBalanceWithVersion(
                                    mainAccountId, shard.getBalance(),
                                    freshAccount.getVersion(), LocalDateTime.now());
                            if (updated == 0) {
                                prometheusConfig.recordHotAccountConflict();
                                throw new BusinessException(ResultCodeEnum.CONCURRENT_UPDATE_FAILED);
                            }
                            return null;
                        });
                    } catch (Exception e) {
                        prometheusConfig.recordHotAccountConflict();
                        throw e;
                    }

                    accountShardMapper.resetBalanceAfterMerge(shard.getShardId(), LocalDateTime.now(), LocalDateTime.now());
                }

                String cacheKey = CommonConstants.SHARD_ACCOUNT_CACHE_PREFIX + mainAccountId;
                redissonClient.getBucket(cacheKey).delete();

                log.info("影子账户归并完成, mainAccountId: {}, 归并金额: {}分", mainAccountId, totalAmount);
                return totalAmount;
            });
        });
        return result[0];
    }

    @Override
    @Scheduled(cron = "${account.hot.shard-merge-cron:" + CommonConstants.SHARD_MERGE_CRON + "}")
    public void mergeAllShards() {
        log.info("开始定时归并所有热点账户影子分片");

        // 使用全局分布式锁防止多实例同时执行归并任务
        // 锁持有时间1小时，防止任务执行时间过长
        String lockKey = "hot:account:merge:all:lock";
        DistributedLockUtil.executeWithLock(lockKey, 1L, 3600L, () -> {
            // 查询所有热点账户（状态为2-已分片 或 3-已启用缓冲）
            List<Account> hotAccounts = accountMapper.selectAllHotAccounts();
            if (hotAccounts.isEmpty()) {
                log.info("没有需要归并的热点账户");
                return null;
            }

            log.info("找到{}个热点账户需要归并", hotAccounts.size());

            // 统计归并结果
            int successCount = 0;
            int failCount = 0;
            long totalMergedAmount = 0L;

            // 逐个归并每个热点账户
            // 注意：单个账户归并失败不影响其他账户
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
        // 查询主账户信息
        Account mainAccount = accountMapper.selectByAccountId(mainAccountId);
        if (mainAccount == null) {
            return 0L;
        }

        // 初始化为主账户余额
        long total = mainAccount.getBalance() != null ? mainAccount.getBalance() : 0L;

        // 如果是热点账户，需要累加所有影子账户的余额
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
        // 查询主账户的所有影子账户（包含已关闭的）
        return accountShardMapper.selectByMainAccountId(mainAccountId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsHotAccount(String accountId, Integer shardCount) {
        log.info("标记账户为热点账户, accountId: {}, shardCount: {}", accountId, shardCount);

        // 校验账户是否存在
        Account account = accountMapper.selectByAccountId(accountId);
        if (account == null) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST);
        }

        // 只有正常状态的账户才能标记为热点账户
        if (!AccountStatusEnum.NORMAL.getCode().equals(account.getStatus())) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_STATUS_ERROR, "账户状态异常");
        }

        // 创建影子账户分片，默认使用随机路由策略
        createShards(accountId, shardCount, ShardingStrategyEnum.RANDOM.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unmarkAsHotAccount(String accountId) {
        log.info("取消热点账户标记, accountId: {}", accountId);

        // 第一步：先归并所有影子账户的余额到主账户
        mergeShards(accountId);

        // 第二步：更新主账户状态为普通账户
        Account account = accountMapper.selectByAccountId(accountId);
        if (account != null) {
            account.setHotStatus(HotAccountStatusEnum.NORMAL.getCode());
            account.setUpdateTime(LocalDateTime.now());
            accountMapper.updateById(account);
        }

        // 第三步：关闭所有影子账户（状态置为1）
        List<AccountShard> shards = accountShardMapper.selectByMainAccountId(accountId);
        for (AccountShard shard : shards) {
            shard.setStatus(1);
            shard.setUpdateTime(LocalDateTime.now());
            accountShardMapper.updateById(shard);
        }

        // 第四步：清除相关缓存，确保下次查询获取最新状态
        String cacheKey = CommonConstants.HOT_ACCOUNT_LOCK_PREFIX + accountId;
        redissonClient.getBucket(cacheKey).delete();
        cacheKey = CommonConstants.SHARD_ACCOUNT_CACHE_PREFIX + accountId;
        redissonClient.getBucket(cacheKey).delete();

        log.info("热点账户标记取消完成, accountId: {}", accountId);
    }

    /**
     * 从缓存获取活跃分片列表
     * 使用FastJSON反序列化缓存的JSON字符串
     *
     * @param mainAccountId 主账户ID
     * @return 活跃分片列表，缓存未命中返回null
     */
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

    /**
     * 随机路由策略
     * 使用SecureRandom生成随机索引，选择分片
     * 优点：实现简单，分布均匀
     * 缺点：无法保证同一请求路由到同一分片
     *
     * @param shards 活跃分片列表
     * @return 选中的分片
     */
    private AccountShard selectRandomShard(List<AccountShard> shards) {
        int index = secureRandom.nextInt(shards.size());
        return shards.get(index);
    }

    /**
     * 轮询路由策略
     * 使用AtomicInteger保证原子性递增，按顺序轮询选择分片
     * 优点：请求分布绝对均匀
     * 缺点：多实例部署时轮询计数器不同步，可能不均匀
     *
     * @param shards 活跃分片列表
     * @return 选中的分片
     */
    private AccountShard selectRoundRobinShard(List<AccountShard> shards) {
        int index = roundRobinCounter.getAndIncrement() % shards.size();
        // 防止计数器溢出变为负数，重置为0
        if (index < 0) {
            index = 0;
            roundRobinCounter.set(0);
        }
        return shards.get(index);
    }

    /**
     * 哈希路由策略
     * 根据key的哈希值选择分片，保证同一key始终路由到同一分片
     * 优点：同一请求/线程始终路由到同一分片，便于追踪
     * 缺点：如果某些分片特别热门，可能导致负载不均
     *
     * @param shards 活跃分片列表
     * @param key 哈希键值（如线程名、用户ID等）
     * @return 选中的分片
     */
    private AccountShard selectHashShard(List<AccountShard> shards, String key) {
        // 使用Math.abs确保哈希值为正数
        int hash = Math.abs(key.hashCode());
        int index = hash % shards.size();
        return shards.get(index);
    }
}
