package com.bank.core.account.compensation.impl;

import com.alibaba.fastjson.JSON;
import com.bank.core.account.compensation.TransactionCompensator;
import com.bank.core.account.entity.SagaTransactionLog;
import com.bank.core.account.mapper.SagaTransactionLogMapper;
import com.bank.core.account.saga.SagaAction;
import com.bank.core.account.saga.SagaCoordinator;
import com.bank.core.common.enums.SagaStepStatusEnum;
import com.bank.core.common.enums.TransactionPhaseEnum;
import com.bank.core.common.utils.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionCompensatorImpl implements TransactionCompensator {

    private final SagaTransactionLogMapper sagaLogMapper;
    private final SagaCoordinator sagaCoordinator;
    private final RedissonClient redissonClient;
    private final Map<String, SagaAction> actionMap = new ConcurrentHashMap<>();

    private static final String COMPENSATE_LOCK_PREFIX = "compensate:lock:";
    private static final int DEFAULT_COMPENSATE_HOURS = 24;
    private static final int MAX_COMPENSATE_RETRY = 5;

    public void registerAction(SagaAction action) {
        actionMap.put(action.getServiceName(), action);
        log.info("注册补偿动作: {}", action.getServiceName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean compensate(String transactionId, Map<String, Object> params) {
        log.info("手动补偿事务, transactionId={}", transactionId);

        String lockKey = COMPENSATE_LOCK_PREFIX + transactionId;
        RLock lock = redissonClient.getFairLock(lockKey);

        try {
            if (!lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                log.warn("获取补偿锁失败, transactionId={}", transactionId);
                return false;
            }

            List<SagaTransactionLog> logs = sagaLogMapper.selectBySagaId(transactionId);
            if (logs.isEmpty()) {
                log.warn("未找到事务日志, transactionId={}", transactionId);
                return false;
            }

            boolean allSuccess = true;
            for (int i = logs.size() - 1; i >= 0; i--) {
                SagaTransactionLog logEntry = logs.get(i);
                if (SagaStepStatusEnum.FORWARD_SUCCESS.getCode().equals(logEntry.getStepStatus())
                        && TransactionPhaseEnum.FORWARD.getCode().equals(logEntry.getPhase())) {
                    try {
                        boolean success = compensate(logEntry);
                        if (!success) {
                            allSuccess = false;
                        }
                    } catch (Exception e) {
                        log.error("补偿步骤失败, transactionId={}, stepId={}", transactionId, logEntry.getStepId(), e);
                        allSuccess = false;
                    }
                }
            }

            log.info("手动补偿事务完成, transactionId={}, result={}", transactionId, allSuccess);
            return allSuccess;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("补偿线程被中断, transactionId={}", transactionId, e);
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean compensate(SagaTransactionLog logEntry) {
        String sagaId = logEntry.getSagaId();
        String stepId = logEntry.getStepId();
        String stepName = logEntry.getStepName();

        log.info("补偿Saga步骤, sagaId={}, stepId={}, stepName={}", sagaId, stepId, stepName);

        SagaTransactionLog latestLog = sagaLogMapper.selectLatestStepLog(sagaId, stepId);
        if (latestLog != null
                && SagaStepStatusEnum.COMPENSATE_SUCCESS.getCode().equals(latestLog.getStepStatus())) {
            log.warn("步骤已补偿成功, sagaId={}, stepId={}", sagaId, stepId);
            return true;
        }

        if (latestLog != null
                && latestLog.getRetryCount() != null
                && latestLog.getRetryCount() >= MAX_COMPENSATE_RETRY) {
            log.error("步骤补偿重试次数超限, sagaId={}, stepId={}, retryCount={}",
                    sagaId, stepId, latestLog.getRetryCount());
            return false;
        }

        String serviceName = extractServiceName(stepName);
        SagaAction action = actionMap.get(serviceName);
        if (action == null) {
            log.error("未找到补偿动作, serviceName={}", serviceName);
            return false;
        }

        try {
            Map<String, Object> params = JSON.parseObject(logEntry.getParams(), Map.class);
            boolean success = action.compensate(sagaId, params);

            SagaTransactionLog compensateLog = new SagaTransactionLog();
            compensateLog.setId(SnowflakeIdGenerator.nextId());
            compensateLog.setSagaId(sagaId);
            compensateLog.setSagaName(logEntry.getSagaName());
            compensateLog.setBusinessNo(logEntry.getBusinessNo());
            compensateLog.setTransactionType(logEntry.getTransactionType());
            compensateLog.setStatus(logEntry.getStatus());
            compensateLog.setStepId(stepId);
            compensateLog.setStepName(stepName);
            compensateLog.setPhase(TransactionPhaseEnum.COMPENSATE.getCode());
            compensateLog.setStepStatus(success
                    ? SagaStepStatusEnum.COMPENSATE_SUCCESS.getCode()
                    : SagaStepStatusEnum.COMPENSATE_FAILED.getCode());
            compensateLog.setRetryCount(latestLog != null ? latestLog.getRetryCount() + 1 : 1);
            compensateLog.setParams(logEntry.getParams());
            compensateLog.setExecuteTime(LocalDateTime.now());
            compensateLog.setCompleteTime(LocalDateTime.now());
            compensateLog.setCreateTime(LocalDateTime.now());
            compensateLog.setUpdateTime(LocalDateTime.now());
            sagaLogMapper.insert(compensateLog);

            if (success) {
                log.info("补偿步骤成功, sagaId={}, stepId={}", sagaId, stepId);
            } else {
                log.warn("补偿步骤失败, sagaId={}, stepId={}", sagaId, stepId);
            }

            return success;

        } catch (Exception e) {
            log.error("补偿步骤异常, sagaId={}, stepId={}", sagaId, stepId, e);

            SagaTransactionLog errorLog = new SagaTransactionLog();
            errorLog.setId(SnowflakeIdGenerator.nextId());
            errorLog.setSagaId(sagaId);
            errorLog.setSagaName(logEntry.getSagaName());
            errorLog.setBusinessNo(logEntry.getBusinessNo());
            errorLog.setTransactionType(logEntry.getTransactionType());
            errorLog.setStatus(logEntry.getStatus());
            errorLog.setStepId(stepId);
            errorLog.setStepName(stepName);
            errorLog.setPhase(TransactionPhaseEnum.COMPENSATE.getCode());
            errorLog.setStepStatus(SagaStepStatusEnum.COMPENSATE_FAILED.getCode());
            errorLog.setRetryCount(latestLog != null ? latestLog.getRetryCount() + 1 : 1);
            errorLog.setErrorMessage(e.getMessage());
            errorLog.setParams(logEntry.getParams());
            errorLog.setExecuteTime(LocalDateTime.now());
            errorLog.setCompleteTime(LocalDateTime.now());
            errorLog.setCreateTime(LocalDateTime.now());
            errorLog.setUpdateTime(LocalDateTime.now());
            sagaLogMapper.insert(errorLog);

            return false;
        }
    }

    @Override
    public List<SagaTransactionLog> findFailedTransactions(int hours) {
        LocalDateTime timeThreshold = LocalDateTime.now().minusHours(hours);
        return sagaLogMapper.selectPendingTransactions(timeThreshold);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean retryCompensate(String transactionId) {
        log.info("重试补偿事务, transactionId={}", transactionId);

        List<SagaTransactionLog> logs = sagaLogMapper.selectBySagaId(transactionId);
        if (logs.isEmpty()) {
            log.warn("未找到事务日志, transactionId={}", transactionId);
            return false;
        }

        boolean allSuccess = true;
        for (SagaTransactionLog logEntry : logs) {
            if (SagaStepStatusEnum.COMPENSATE_FAILED.getCode().equals(logEntry.getStepStatus())) {
                boolean success = compensate(logEntry);
                if (!success) {
                    allSuccess = false;
                }
            }
        }

        log.info("重试补偿事务完成, transactionId={}, result={}", transactionId, allSuccess);
        return allSuccess;
    }

    @Override
    @Scheduled(fixedDelay = 300000)
    public void autoCompensateFailedTransactions() {
        log.info("开始自动补偿失败事务");

        List<SagaTransactionLog> failedLogs = findFailedTransactions(DEFAULT_COMPENSATE_HOURS);
        if (failedLogs.isEmpty()) {
            log.info("无需要补偿的失败事务");
            return;
        }

        log.info("发现{}条需要补偿的失败事务", failedLogs.size());

        for (SagaTransactionLog logEntry : failedLogs) {
            try {
                compensate(logEntry);
            } catch (Exception e) {
                log.error("自动补偿失败, sagaId={}", logEntry.getSagaId(), e);
            }
        }

        log.info("自动补偿失败事务完成");
    }

    private String extractServiceName(String stepName) {
        if (stepName == null) {
            return null;
        }
        if (stepName.contains("跨行转账")) {
            return "crossBankTransfer";
        }
        if (stepName.contains("退款")) {
            return "refund";
        }
        return stepName;
    }
}
