package com.bank.core.account.saga;

import com.alibaba.fastjson.JSON;
import com.bank.core.account.entity.SagaTransactionLog;
import com.bank.core.account.mapper.SagaTransactionLogMapper;
import com.bank.core.common.enums.DistributedTransactionTypeEnum;
import com.bank.core.common.enums.SagaStepStatusEnum;
import com.bank.core.common.enums.TransactionPhaseEnum;
import com.bank.core.common.enums.TransactionStatusEnum;
import com.bank.core.common.utils.SnowflakeIdGenerator;
import io.seata.saga.engine.StateMachineEngine;
import io.seata.saga.statelang.domain.StateMachineInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationContext;
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
public class SagaCoordinator {

    private final SagaTransactionLogMapper sagaLogMapper;
    private final RedissonClient redissonClient;
    private final ApplicationContext applicationContext;
    private final Map<String, SagaAction> actionMap = new ConcurrentHashMap<>();

    public void registerAction(SagaAction action) {
        actionMap.put(action.getServiceName(), action);
        log.info("注册Saga动作: {}", action.getServiceName());
    }

    @Transactional(rollbackFor = Exception.class)
    public String executeSaga(SagaTransaction saga) {
        String sagaId = SnowflakeIdGenerator.nextIdStr();
        saga.setSagaId(sagaId);
        saga.setCreateTime(LocalDateTime.now());
        saga.setStatus(TransactionStatusEnum.PENDING.getCode());

        log.info("开始执行Saga事务, sagaId={}, sagaName={}, businessNo={}",
                sagaId, saga.getSagaName(), saga.getBusinessNo());

        saveSagaLog(saga, null, TransactionPhaseEnum.FORWARD, SagaStepStatusEnum.PENDING);

        try {
            while (!saga.isCompleted()) {
                SagaStep step = saga.getCurrentStep();
                step.setExecuteTime(LocalDateTime.now());
                step.setStatus(SagaStepStatusEnum.PENDING.getCode());

                saveSagaLog(saga, step, TransactionPhaseEnum.FORWARD, SagaStepStatusEnum.PENDING);

                SagaAction action = actionMap.get(step.getServiceName());
                if (action == null) {
                    throw new RuntimeException("未找到Saga动作: " + step.getServiceName());
                }

                boolean success = false;
                String errorMsg = null;

                while (step.getRetryCount() < step.getMaxRetryCount()) {
                    try {
                        log.info("执行Saga步骤正向操作, sagaId={}, step={}, 第{}次重试",
                                sagaId, step.getStepName(), step.getRetryCount() + 1);
                        success = action.forward(sagaId, step.getParams());
                        if (success) {
                            break;
                        }
                    } catch (Exception e) {
                        errorMsg = e.getMessage();
                        log.error("Saga步骤执行失败, sagaId={}, step={}, error={}",
                                sagaId, step.getStepName(), e.getMessage(), e);
                    }
                    step.setRetryCount(step.getRetryCount() + 1);
                }

                if (success) {
                    step.setStatus(SagaStepStatusEnum.FORWARD_SUCCESS.getCode());
                    step.setCompleteTime(LocalDateTime.now());
                    saveSagaLog(saga, step, TransactionPhaseEnum.FORWARD, SagaStepStatusEnum.FORWARD_SUCCESS);
                    saga.moveToNextStep();
                } else {
                    step.setStatus(SagaStepStatusEnum.FORWARD_FAILED.getCode());
                    step.setErrorMessage(errorMsg);
                    step.setCompleteTime(LocalDateTime.now());
                    saveSagaLog(saga, step, TransactionPhaseEnum.FORWARD, SagaStepStatusEnum.FORWARD_FAILED);

                    log.warn("Saga正向操作失败, 开始补偿, sagaId={}, step={}", sagaId, step.getStepName());
                    saga.setCompensating(true);
                    saga.setErrorMessage(errorMsg);

                    compensateSaga(saga);
                    break;
                }
            }

            if (saga.getCompensating()) {
                saga.setStatus(TransactionStatusEnum.FAILED.getCode());
            } else {
                saga.setStatus(TransactionStatusEnum.SUCCESS.getCode());
            }
            saga.setCompleteTime(LocalDateTime.now());
            saga.setUpdateTime(LocalDateTime.now());

            saveSagaLog(saga, null, TransactionPhaseEnum.FORWARD,
                    saga.getStatus() == TransactionStatusEnum.SUCCESS.getCode()
                            ? SagaStepStatusEnum.FORWARD_SUCCESS
                            : SagaStepStatusEnum.COMPENSATE_SUCCESS);

            String cacheKey = "saga:transaction:" + sagaId;
            redissonClient.getBucket(cacheKey).set(JSON.toJSONString(saga), 24, TimeUnit.HOURS);

            log.info("Saga事务执行完成, sagaId={}, status={}",
                    sagaId,
                    saga.getStatus() == TransactionStatusEnum.SUCCESS.getCode() ? "成功" : "失败");

            return sagaId;
        } catch (Exception e) {
            log.error("Saga事务执行异常, sagaId={}", sagaId, e);
            saga.setStatus(TransactionStatusEnum.FAILED.getCode());
            saga.setErrorMessage(e.getMessage());
            saga.setCompleteTime(LocalDateTime.now());
            saveSagaLog(saga, null, TransactionPhaseEnum.FORWARD, SagaStepStatusEnum.FORWARD_FAILED);

            if (!saga.getCompensating()) {
                saga.setCompensating(true);
                compensateSaga(saga);
            }
            throw e;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void compensateSaga(SagaTransaction saga) {
        String sagaId = saga.getSagaId();
        log.info("开始Saga补偿, sagaId={}", sagaId);

        while (!saga.isAllCompensated()) {
            SagaStep step = saga.getCurrentStep();
            step.setExecuteTime(LocalDateTime.now());

            saveSagaLog(saga, step, TransactionPhaseEnum.COMPENSATE, SagaStepStatusEnum.PENDING);

            SagaAction action = actionMap.get(step.getServiceName());

            boolean success = false;
            String errorMsg = null;

            while (step.getRetryCount() < step.getMaxRetryCount() + 3) {
                try {
                    log.info("执行Saga补偿操作, sagaId={}, step={}, 第{}次重试",
                            sagaId, step.getStepName(), step.getRetryCount() + 1);
                    success = action.compensate(sagaId, step.getParams());
                    if (success) {
                        break;
                    }
                } catch (Exception e) {
                    errorMsg = e.getMessage();
                    log.error("Saga补偿执行失败, sagaId={}, step={}, error={}",
                            sagaId, step.getStepName(), e.getMessage(), e);
                }
                step.setRetryCount(step.getRetryCount() + 1);
            }

            if (success) {
                step.setStatus(SagaStepStatusEnum.COMPENSATE_SUCCESS.getCode());
                step.setCompleteTime(LocalDateTime.now());
                saveSagaLog(saga, step, TransactionPhaseEnum.COMPENSATE, SagaStepStatusEnum.COMPENSATE_SUCCESS);

                if (!saga.moveToPrevStep()) {
                    break;
                }
            } else {
                step.setStatus(SagaStepStatusEnum.COMPENSATE_FAILED.getCode());
                step.setErrorMessage(errorMsg);
                step.setCompleteTime(LocalDateTime.now());
                saveSagaLog(saga, step, TransactionPhaseEnum.COMPENSATE, SagaStepStatusEnum.COMPENSATE_FAILED);

                log.error("Saga补偿失败, 需要人工干预, sagaId={}, step={}", sagaId, step.getStepName());
                break;
            }
        }

        log.info("Saga补偿完成, sagaId={}, 补偿结果: {}", sagaId,
                saga.isAllCompensated() ? "全部补偿成功" : "部分补偿失败，需要人工干预");
    }

    private void saveSagaLog(SagaTransaction saga, SagaStep step,
                               TransactionPhaseEnum phase, SagaStepStatusEnum status) {
        SagaTransactionLog logEntry = new SagaTransactionLog();
        logEntry.setId(SnowflakeIdGenerator.nextId());
        logEntry.setSagaId(saga.getSagaId());
        logEntry.setSagaName(saga.getSagaName());
        logEntry.setBusinessNo(saga.getBusinessNo());
        logEntry.setTransactionType(DistributedTransactionTypeEnum.SAGA.getCode());
        logEntry.setStatus(saga.getStatus());
        logEntry.setPhase(phase.getCode());
        logEntry.setStepStatus(status.getCode());
        logEntry.setRetryCount(step != null ? step.getRetryCount() : 0);
        logEntry.setErrorMessage(step != null ? step.getErrorMessage() : saga.getErrorMessage());
        logEntry.setParams(step != null ? JSON.toJSONString(step.getParams()) : null);
        logEntry.setExecuteTime(step != null ? step.getExecuteTime() : LocalDateTime.now());
        logEntry.setCompleteTime(step != null ? step.getCompleteTime() : null);
        logEntry.setCreateTime(LocalDateTime.now());
        logEntry.setUpdateTime(LocalDateTime.now());

        if (step != null) {
            logEntry.setStepId(step.getStepId());
            logEntry.setStepName(step.getStepName());
        }

        sagaLogMapper.insert(logEntry);
    }

    public SagaTransaction getSagaTransaction(String sagaId) {
        String cacheKey = "saga:transaction:" + sagaId;
        RBucket<String> cacheBucket = redissonClient.getBucket(cacheKey);
        String cached = cacheBucket.get();
        if (cached != null) {
            return JSON.parseObject(cached, SagaTransaction.class);
        }

        List<SagaTransactionLog> logs = sagaLogMapper.selectBySagaId(sagaId);
        if (logs.isEmpty()) {
            return null;
        }

        SagaTransaction saga = new SagaTransaction();
        saga.setSagaId(sagaId);
        saga.setSagaName(logs.get(0).getSagaName());
        saga.setBusinessNo(logs.get(0).getBusinessNo());
        saga.setStatus(logs.get(logs.size() - 1).getStatus());
        saga.setCreateTime(logs.get(0).getCreateTime());
        saga.setUpdateTime(logs.get(logs.size() - 1).getUpdateTime());

        return saga;
    }

    public List<SagaTransactionLog> getSagaLogs(String sagaId) {
        return sagaLogMapper.selectBySagaId(sagaId);
    }
}
