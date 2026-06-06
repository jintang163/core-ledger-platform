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

        AccountBufferLog existing = bufferLogMapper.selectByRequestId(requestId);
        if (existing != null) {
            log.warn("缓冲流水已存在（幂等命中）, requestId: {}, bufferId: {}", requestId, existing.getBufferId());
            return existing.getBufferId();
        }

        existing = bufferLogMapper.selectByBusinessNo(businessNo);
        if (existing != null) {
            log.warn("缓冲流水已存在（幂等命中）, businessNo: {}, bufferId: {}", businessNo, existing.getBufferId());
            return existing.getBufferId();
        }

        Account account = accountMapper.selectByAccountId(accountId);
        if (account == null) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST);
        }

        long amountFen = AmountUtil.yuanToFen(amount);

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

        String cacheKey = CommonConstants.BUFFER_LOG_CACHE_PREFIX + bufferLog.getBufferId();
        redissonClient.getBucket(cacheKey).set(JSON.toJSONString(bufferLog), 5, TimeUnit.MINUTES);

        log.info("缓冲流水记录成功, bufferId: {}, accountId: {}, amount: {}分",
                bufferLog.getBufferId(), accountId, amountFen);

        return bufferLog.getBufferId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int processBufferLogs(int batchSize) {
        String lockKey = "buffer:process:lock";
        return DistributedLockUtil.executeWithLock(lockKey, 1L, 30L, () -> {
            List<AccountBufferLog> pendingLogs = bufferLogMapper.selectPendingLogs(
                    BufferStatusEnum.PENDING.getCode(), batchSize);

            if (pendingLogs.isEmpty()) {
                return 0;
            }

            String batchNo = "BATCH" + SnowflakeIdGenerator.nextIdStr();
            List<String> bufferIds = pendingLogs.stream()
                    .map(AccountBufferLog::getBufferId)
                    .toList();

            bufferLogMapper.batchUpdateStatus(bufferIds, BufferStatusEnum.PROCESSING.getCode(),
                    batchNo, LocalDateTime.now());

            int successCount = 0;
            for (AccountBufferLog log : pendingLogs) {
                try {
                    processSingleBufferLog(log, batchNo);
                    successCount++;
                } catch (Exception e) {
                    log.error("处理缓冲流水失败, bufferId: {}", log.getBufferId(), e);
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
            processBufferLogs(CommonConstants.BUFFER_BATCH_SIZE);
        } catch (Exception e) {
            log.error("定时处理缓冲流水异常", e);
        }
    }

    @Override
    public Long getPendingAmount(String accountId) {
        return bufferLogMapper.sumPendingAmountByAccountId(accountId);
    }

    @Override
    public Long getAvailableBalance(String accountId) {
        Account account = accountMapper.selectByAccountId(accountId);
        if (account == null) {
            return 0L;
        }

        long accountBalance = account.getBalance() != null ? account.getBalance() : 0L;
        long pendingAmount = getPendingAmount(accountId);

        return accountBalance + pendingAmount;
    }

    @Override
    public AccountBufferLog getBufferLog(String bufferId) {
        String cacheKey = CommonConstants.BUFFER_LOG_CACHE_PREFIX + bufferId;
        RBucket<String> cacheBucket = redissonClient.getBucket(cacheKey);
        String cacheValue = cacheBucket.get();
        if (cacheValue != null) {
            return JSON.parseObject(cacheValue, AccountBufferLog.class);
        }

        AccountBufferLog log = bufferLogMapper.selectByBufferId(bufferId);
        if (log != null) {
            cacheBucket.set(JSON.toJSONString(log), 5, TimeUnit.MINUTES);
        }
        return log;
    }

    @Override
    public AccountBufferLog getBufferLogByBusinessNo(String businessNo) {
        return bufferLogMapper.selectByBusinessNo(businessNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean retryProcessBufferLog(String bufferId) {
        log.info("重试处理缓冲流水, bufferId: {}", bufferId);

        AccountBufferLog bufferLog = bufferLogMapper.selectByBufferId(bufferId);
        if (bufferLog == null) {
            throw new BusinessException(ResultCodeEnum.BUFFER_LOG_NOT_EXIST);
        }

        if (BufferStatusEnum.SUCCESS.getCode().equals(bufferLog.getStatus())) {
            log.warn("缓冲流水已处理成功，无需重试, bufferId: {}", bufferId);
            return true;
        }

        if (bufferLog.getRetryCount() >= CommonConstants.MAX_RETRY_TIMES) {
            throw new BusinessException(ResultCodeEnum.BUFFER_LOG_RETRY_EXCEEDED,
                    "已超过最大重试次数: " + CommonConstants.MAX_RETRY_TIMES);
        }

        try {
            processSingleBufferLog(bufferLog, "RETRY" + SnowflakeIdGenerator.nextIdStr());
            return true;
        } catch (Exception e) {
            log.error("重试处理缓冲流水失败, bufferId: {}", bufferId, e);
            handleProcessFailure(bufferLog, "RETRY" + SnowflakeIdGenerator.nextIdStr(), e.getMessage());
            return false;
        }
    }

    @Override
    public List<AccountBufferLog> getPendingLogs(int limit) {
        return bufferLogMapper.selectPendingLogs(BufferStatusEnum.PENDING.getCode(), limit);
    }

    @Override
    public boolean isBufferEnabled(String accountId) {
        Account account = accountMapper.selectByAccountId(accountId);
        if (account == null || account.getHotStatus() == null) {
            return false;
        }
        return HotAccountStatusEnum.HOT_BUFFER.getCode().equals(account.getHotStatus());
    }

    private void processSingleBufferLog(AccountBufferLog bufferLog, String batchNo) {
        log.debug("处理单条缓冲流水, bufferId: {}, accountId: {}, amount: {}分",
                bufferLog.getBufferId(), bufferLog.getAccountId(), bufferLog.getAmountFen());

        Account account = accountMapper.selectByAccountId(bufferLog.getAccountId());
        if (account == null) {
            throw new BusinessException(ResultCodeEnum.ACCOUNT_NOT_EXIST);
        }

        if (bufferLog.getAmountFen() < 0) {
            long newBalance = account.getBalance() + bufferLog.getAmountFen();
            if (newBalance < 0) {
                throw new BusinessException(ResultCodeEnum.INSUFFICIENT_BALANCE,
                        "账户余额不足, accountId: " + bufferLog.getAccountId() +
                                ", 余额: " + AmountUtil.fenToYuan(account.getBalance()) +
                                ", 需要: " + AmountUtil.fenToYuan(-bufferLog.getAmountFen()));
            }
        }

        RetryUtil.executeWithRetry(() -> {
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

        bufferLogMapper.updateStatus(
                bufferLog.getBufferId(),
                BufferStatusEnum.SUCCESS.getCode(),
                batchNo,
                null,
                LocalDateTime.now(),
                null,
                LocalDateTime.now()
        );

        String cacheKey = CommonConstants.ACCOUNT_CACHE_PREFIX + bufferLog.getAccountId();
        redissonClient.getBucket(cacheKey).delete();
    }

    private void handleProcessFailure(AccountBufferLog bufferLog, String batchNo, String errorMsg) {
        int newRetryCount = bufferLog.getRetryCount() + 1;
        Integer newStatus = newRetryCount >= CommonConstants.MAX_RETRY_TIMES
                ? BufferStatusEnum.FAILED.getCode()
                : BufferStatusEnum.PENDING.getCode();

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
