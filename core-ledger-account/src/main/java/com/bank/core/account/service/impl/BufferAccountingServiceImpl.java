package com.bank.core.account.service.impl;

import com.alibaba.fastjson.JSON;
import com.bank.core.account.entity.Account;
import com.bank.core.account.entity.AccountBufferLog;
import com.bank.core.account.mapper.AccountBufferLogMapper;
import com.bank.core.account.mapper.AccountMapper;
import com.bank.core.account.service.BufferAccountingService;
import com.bank.core.common.constants.CommonConstants;
import com.bank.core.common.enums.BufferStatusEnum;
import com.bank.core.common.enums.HotAccountStatusEnum;
import com.bank.core.common.enums.ResultCodeEnum;
import com.bank.core.common.exception.BusinessException;
import com.bank.core.common.utils.AmountUtil;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 缓冲记账服务实现类
 *
 * 核心功能：
 * 1. 先记录流水：高并发请求时，先写入缓冲流水表，立即返回成功
 * 2. 异步批量更新：后台定时任务批量处理流水，更新账户余额
 * 3. 余额聚合：查询余额时自动聚合账户余额和待处理缓冲金额
 *
 * 适用场景：
 * - 允许短暂数据不一致（最多延迟5秒）
 * - 高并发批量操作（如批量代发、批量扣款）
 * - 非实时性要求的账户操作
 *
 * 设计要点：
 * - 流水记录支持幂等（按requestId和businessNo去重）
 * - 定时任务每秒处理一次缓冲流水
 * - 每次处理100条流水
 * - 支持失败重试（最多3次）
 * - 余额查询时自动计算待处理金额
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BufferAccountingServiceImpl implements BufferAccountingService {

    private final AccountBufferLogMapper bufferLogMapper;
    private final AccountMapper accountMapper;
    private final RedissonClient redissonClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String recordBufferLog(String requestId, String businessNo, String accountId,
                                   BigDecimal amount, String currency, Integer transactionType,
                                   String remark, String operator) {
        log.info("记录缓冲流水, accountId: {}, amount: {}", accountId, amount);

        // 幂等校验第一步：按requestId查询，防止重复请求
        AccountBufferLog existing = bufferLogMapper.selectByRequestId(requestId);
        if (existing != null) {
            log.warn("缓冲流水已存在（幂等命中）, requestId: {}, bufferId: {}", requestId, existing.getBufferId());
            return existing.getBufferId();
        }

        // 幂等校验第二步：按businessNo查询，防止重复业务流水
        existing = bufferLogMapper.selectByBusinessNo(businessNo);
        if (existing != null) {
            log.warn("缓冲流水已存在（幂等命中）, businessNo: {}, bufferId: {}", businessNo, existing.getBufferId());
            return existing.getBufferId();
        }

        // 校验账户是否存在
        Account account = accountMapper.selectByAccountId(accountId);
        if (account == null) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST);
        }

        // 金额单位转换：元 -> 分
        long amountFen = AmountUtil.yuanToFen(amount);

        // 构造缓冲流水记录，状态初始化为"待处理"
        AccountBufferLog bufferLog = new AccountBufferLog();
        bufferLog.setId(SnowflakeIdGenerator.nextId());
        bufferLog.setBufferId(SnowflakeIdGenerator.generateBufferId());
        bufferLog.setBusinessNo(businessNo);
        bufferLog.setRequestId(requestId);
        bufferLog.setAccountId(accountId);
        bufferLog.setAccountNo(account.getAccountNo());
        bufferLog.setAmount(amount);
        bufferLog.setAmountFen(amountFen);
        bufferLog.setCurrency(currency);
        bufferLog.setTransactionType(transactionType);
        bufferLog.setStatus(BufferStatusEnum.PENDING.getCode());
        bufferLog.setRetryCount(0);
        bufferLog.setRemark(remark);
        bufferLog.setOperator(operator);
        bufferLog.setCreateTime(LocalDateTime.now());
        bufferLog.setUpdateTime(LocalDateTime.now());
        bufferLog.setDeleted(0);

        bufferLogMapper.insert(bufferLog);

        // 写入缓存，便于快速查询
        String cacheKey = CommonConstants.BUFFER_LOG_CACHE_PREFIX + bufferLog.getBufferId();
        redissonClient.getBucket(cacheKey).set(JSON.toJSONString(bufferLog), 5, TimeUnit.MINUTES);

        log.info("缓冲流水记录成功, bufferId: {}, accountId: {}, amount: {}分",
                bufferLog.getBufferId(), accountId, amountFen);

        return bufferLog.getBufferId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int processBufferLogs(int batchSize) {
        // 使用全局分布式锁防止多实例同时处理缓冲流水
        // 锁等待时间1秒，持有时间30秒
        String lockKey = "buffer:process:lock";
        return DistributedLockUtil.executeWithLock(lockKey, 1L, 30L, () -> {
            // 查询待处理的缓冲流水，按创建时间升序（先进先出）
            List<AccountBufferLog> pendingLogs = bufferLogMapper.selectPendingLogs(
                    BufferStatusEnum.PENDING.getCode(), batchSize);

            if (pendingLogs.isEmpty()) {
                return 0;
            }

            // 生成本批次号，便于追踪
            String batchNo = "BATCH" + SnowflakeIdGenerator.nextIdStr();
            List<String> bufferIds = pendingLogs.stream()
                    .map(AccountBufferLog::getBufferId)
                    .toList();

            // 先批量更新状态为"处理中"，防止其他线程重复处理
            bufferLogMapper.batchUpdateStatus(bufferIds, BufferStatusEnum.PROCESSING.getCode(),
                    batchNo, LocalDateTime.now());

            // 逐个处理缓冲流水
            int successCount = 0;
            for (AccountBufferLog log : pendingLogs) {
                try {
                    processSingleBufferLog(log, batchNo);
                    successCount++;
                } catch (Exception e) {
                    log.error("处理缓冲流水失败, bufferId: {}", log.getBufferId(), e);
                    // 处理失败，更新状态和重试次数
                    handleProcessFailure(log, batchNo, e.getMessage());
                }
            }

            log.info("缓冲流水批量处理完成, batchNo: {}, 总数: {}, 成功: {}, 失败: {}",
                    batchNo, pendingLogs.size(), successCount, pendingLogs.size() - successCount);

            return successCount;
        });
    }

    @Override
    @Scheduled(fixedDelayString = "${account.buffer.process-interval:1000}")
    public void scheduledProcessBufferLogs() {
        try {
            // 定时任务调用批量处理，默认批次大小100
            processBufferLogs(CommonConstants.BUFFER_BATCH_SIZE);
        } catch (Exception e) {
            // 捕获所有异常，防止定时任务中断
            log.error("定时处理缓冲流水异常", e);
        }
    }

    @Override
    public Long getPendingAmount(String accountId) {
        // 汇总账户待处理和处理中的缓冲金额
        return bufferLogMapper.sumPendingAmountByAccountId(accountId);
    }

    @Override
    public Long getAvailableBalance(String accountId) {
        // 查询账户实际余额
        Account account = accountMapper.selectByAccountId(accountId);
        if (account == null) {
            return 0L;
        }

        long accountBalance = account.getBalance() != null ? account.getBalance() : 0L;
        // 查询待处理缓冲金额（可能为正或负）
        long pendingAmount = getPendingAmount(accountId);

        // 可用余额 = 实际余额 + 待处理缓冲金额
        return accountBalance + pendingAmount;
    }

    @Override
    public AccountBufferLog getBufferLog(String bufferId) {
        // 优先从缓存查询
        String cacheKey = CommonConstants.BUFFER_LOG_CACHE_PREFIX + bufferId;
        RBucket<String> cacheBucket = redissonClient.getBucket(cacheKey);
        String cacheValue = cacheBucket.get();
        if (cacheValue != null) {
            return JSON.parseObject(cacheValue, AccountBufferLog.class);
        }

        // 缓存未命中，查询数据库
        AccountBufferLog log = bufferLogMapper.selectByBufferId(bufferId);
        if (log != null) {
            // 写入缓存，有效期5分钟
            cacheBucket.set(JSON.toJSONString(log), 5, TimeUnit.MINUTES);
        }
        return log;
    }

    @Override
    public AccountBufferLog getBufferLogByBusinessNo(String businessNo) {
        // 按业务流水号查询，直接查数据库（不缓存，因为业务流水号可能重复查询但不频繁）
        return bufferLogMapper.selectByBusinessNo(businessNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean retryProcessBufferLog(String bufferId) {
        log.info("重试处理缓冲流水, bufferId: {}", bufferId);

        // 校验流水是否存在
        AccountBufferLog bufferLog = bufferLogMapper.selectByBufferId(bufferId);
        if (bufferLog == null) {
            throw new BusinessException(ResultCodeEnum.BUFFER_LOG_NOT_EXIST);
        }

        // 已成功的流水无需重试
        if (BufferStatusEnum.SUCCESS.getCode().equals(bufferLog.getStatus())) {
            log.warn("缓冲流水已处理成功，无需重试, bufferId: {}", bufferId);
            return true;
        }

        // 检查是否超过最大重试次数
        if (bufferLog.getRetryCount() >= CommonConstants.MAX_RETRY_TIMES) {
            throw new BusinessException(ResultCodeEnum.BUFFER_LOG_RETRY_EXCEEDED,
                    "已超过最大重试次数: " + CommonConstants.MAX_RETRY_TIMES);
        }

        try {
            // 手动重试处理，批次号前缀加RETRY标识
            processSingleBufferLog(bufferLog, "RETRY" + SnowflakeIdGenerator.nextIdStr());
            return true;
        } catch (Exception e) {
            log.error("重试处理缓冲流水失败, bufferId: {}", bufferId, e);
            // 处理失败，更新状态和重试次数
            handleProcessFailure(bufferLog, "RETRY" + SnowflakeIdGenerator.nextIdStr(), e.getMessage());
            return false;
        }
    }

    @Override
    public List<AccountBufferLog> getPendingLogs(int limit) {
        // 查询待处理的缓冲流水列表
        return bufferLogMapper.selectPendingLogs(BufferStatusEnum.PENDING.getCode(), limit);
    }

    @Override
    public boolean isBufferEnabled(String accountId) {
        // 判断账户是否启用了缓冲记账
        // 缓冲记账状态：hot_status = 3
        Account account = accountMapper.selectByAccountId(accountId);
        if (account == null || account.getHotStatus() == null) {
            return false;
        }
        return HotAccountStatusEnum.HOT_BUFFER.getCode().equals(account.getHotStatus());
    }

    /**
     * 处理单条缓冲流水
     * 核心逻辑：校验账户 -> 检查余额（扣款场景）-> 乐观锁更新余额 -> 更新流水状态
     *
     * @param bufferLog 缓冲流水
     * @param batchNo 处理批次号
     */
    private void processSingleBufferLog(AccountBufferLog bufferLog, String batchNo) {
        log.debug("处理单条缓冲流水, bufferId: {}, accountId: {}, amount: {}分",
                bufferLog.getBufferId(), bufferLog.getAccountId(), bufferLog.getAmountFen());

        // 校验账户是否存在
        Account account = accountMapper.selectByAccountId(bufferLog.getAccountId());
        if (account == null) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST);
        }

        // 扣款场景需要检查余额是否充足
        // amountFen < 0 表示扣款，需要确保扣款后余额 >= 0
        if (bufferLog.getAmountFen() < 0) {
            long newBalance = account.getBalance() + bufferLog.getAmountFen();
            if (newBalance < 0) {
                throw new BusinessException(ResultCodeEnum.INSUFFICIENT_BALANCE,
                        "账户余额不足, accountId: " + bufferLog.getAccountId() +
                                ", 余额: " + AmountUtil.fenToYuan(account.getBalance()) +
                                ", 需要: " + AmountUtil.fenToYuan(-bufferLog.getAmountFen()));
            }
        }

        // 使用乐观锁+重试机制更新账户余额
        RetryUtil.executeWithRetry(() -> {
            // 每次重试都需要重新查询最新的账户信息和版本号
            Account freshAccount = accountMapper.selectByAccountId(bufferLog.getAccountId());
            int updated = accountMapper.updateBalanceWithVersion(
                    bufferLog.getAccountId(),
                    bufferLog.getAmountFen(),
                    freshAccount.getVersion(),
                    LocalDateTime.now());
            if (updated == 0) {
                throw new BusinessException(ResultCodeEnum.CONCURRENT_UPDATE_FAILED);
            }
            return null;
        });

        // 更新流水状态为成功，记录处理时间
        bufferLogMapper.updateStatus(
                bufferLog.getBufferId(),
                BufferStatusEnum.SUCCESS.getCode(),
                batchNo,
                null,
                LocalDateTime.now(),
                null,
                LocalDateTime.now()
        );

        // 删除账户缓存，确保下次查询获取最新余额
        String cacheKey = CommonConstants.ACCOUNT_CACHE_PREFIX + bufferLog.getAccountId();
        redissonClient.getBucket(cacheKey).delete();
    }

    /**
     * 处理缓冲流水失败
     * 根据重试次数决定后续状态：
     * - 未超过最大重试次数：状态重置为待处理，等待下次重试
     * - 超过最大重试次数：状态标记为失败，需人工介入
     *
     * @param bufferLog 缓冲流水
     * @param batchNo 处理批次号
     * @param errorMsg 错误信息
     */
    private void handleProcessFailure(AccountBufferLog bufferLog, String batchNo, String errorMsg) {
        // 重试次数+1
        int newRetryCount = bufferLog.getRetryCount() + 1;
        // 判断是否超过最大重试次数，决定新状态
        Integer newStatus = newRetryCount >= CommonConstants.MAX_RETRY_TIMES
                ? BufferStatusEnum.FAILED.getCode()
                : BufferStatusEnum.PENDING.getCode();

        // 更新流水状态、重试次数、错误信息
        bufferLogMapper.updateStatus(
                bufferLog.getBufferId(),
                newStatus,
                batchNo,
                errorMsg,
                LocalDateTime.now(),
                null,
                LocalDateTime.now()
        );
    }
}
