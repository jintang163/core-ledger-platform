package com.bank.core.account.service.impl;

import com.bank.core.account.compensation.TransactionCompensator;
import com.bank.core.account.entity.SagaTransactionLog;
import com.bank.core.account.saga.SagaCoordinator;
import com.bank.core.account.saga.SagaStep;
import com.bank.core.account.saga.SagaTransaction;
import com.bank.core.account.saga.action.CrossBankTransferSagaAction;
import com.bank.core.account.saga.action.RefundSagaAction;
import com.bank.core.account.service.DistributedTransactionService;
import com.bank.core.account.tcc.FreezeTccAction;
import com.bank.core.account.tcc.TransferTccAction;
import com.bank.core.api.dto.AccountFreezeDTO;
import com.bank.core.api.dto.TransferDTO;
import com.bank.core.common.enums.DistributedTransactionTypeEnum;
import com.bank.core.common.utils.SnowflakeIdGenerator;
import io.seata.core.context.RootContext;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分布式事务服务实现类
 * 
 * 核心职责：
 * 1. TCC事务：行内转账、账户冻结
 * 2. Saga事务：跨行转账、原路退款
 * 3. 事务补偿：失败事务的自动/手动补偿
 * 
 * 关键设计：
 * - TCC调用：通过Seata代理Bean调用Try，移除手动new BusinessActionContext()
 * - Seata全局事务：@GlobalTransactional开启全局事务
 * - 上下文持久化：Redis key = tcc:context:{xid}:{branchId}
 * - 幂等性：分布式锁+数据库状态双重校验
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedTransactionServiceImpl implements DistributedTransactionService {

    private final TransferTccAction transferTccAction;
    private final FreezeTccAction freezeTccAction;
    private final SagaCoordinator sagaCoordinator;
    private final TransactionCompensator transactionCompensator;
    private final CrossBankTransferSagaAction crossBankTransferSagaAction;
    private final RefundSagaAction refundSagaAction;

    @PostConstruct
    public void init() {
        sagaCoordinator.registerAction(crossBankTransferSagaAction);
        sagaCoordinator.registerAction(refundSagaAction);
        log.info("分布式事务服务初始化完成，已注册Saga动作");
    }

    /**
     * 行内转账 - TCC模式
     * 
     * 注意：
     * - 通过@GlobalTransactional开启Seata全局事务
     * 通过Seata代理Bean调用transferTccAction.tryTransfer()
     * 不能手动new BusinessActionContext()
     * Seata自动注入BusinessActionContext
     * Confirm/Cancel由Seata事务协调器自动调用
     */
    @Override
    @GlobalTransactional(name = "tcc-transfer", rollbackFor = Exception.class)
    public String transferWithTcc(TransferDTO dto) {
        String xid = RootContext.getXID();
        log.info("开始TCC转账, xid={}, businessNo={}", xid, dto.getBusinessNo());

        try {
            boolean trySuccess = transferTccAction.tryTransfer(
                    null,
                    dto.getBusinessNo(),
                    dto.getFromAccountId(),
                    dto.getToAccountId(),
                    dto.getAmount(),
                    dto.getCurrency(),
                    dto
            );

            if (!trySuccess) {
                throw new RuntimeException("TCC Try阶段失败");
            }

            log.info("TCC转账执行完成, xid={}, businessNo={}", xid, dto.getBusinessNo());
            return xid;
        } catch (Exception e) {
            log.error("TCC转账执行失败, xid={}, businessNo={}", xid, dto.getBusinessNo(), e);
            throw e;
        }
    }

    /**
     * 账户冻结 - TCC模式
     */
    @Override
    @GlobalTransactional(name = "tcc-freeze", rollbackFor = Exception.class)
    public String freezeWithTcc(AccountFreezeDTO dto) {
        String xid = RootContext.getXID();
        log.info("开始TCC冻结, xid={}, accountId={}", xid, dto.getAccountId());

        try {
            boolean trySuccess = freezeTccAction.tryFreeze(
                    null,
                    dto.getAccountId(),
                    dto.getAmount(),
                    dto
            );

            if (!trySuccess) {
                throw new RuntimeException("TCC Try阶段失败");
            }

            log.info("TCC冻结执行完成, xid={}, accountId={}", xid, dto.getAccountId());
            return xid;
        } catch (Exception e) {
            log.error("TCC冻结执行失败, xid={}, accountId={}", xid, dto.getAccountId(), e);
            throw e;
        }
    }

    /**
     * 跨行转账 - Saga模式
     * 拆分为多步骤：
     * 1. 账户资金冻结
     * 2. 渠道出款
     * 3. 清算入账
     * 4. 解冻扣款
     * 失败时逆向补偿
     */
    @Override
    public String executeCrossBankTransferWithSaga(TransferDTO dto) {
        log.info("开始Saga跨行转账, businessNo={}", dto.getBusinessNo());

        SagaTransaction saga = new SagaTransaction();
        saga.setSagaName("跨行转账");
        saga.setBusinessNo(dto.getBusinessNo());
        saga.setTransactionType(DistributedTransactionTypeEnum.SAGA.getCode());

        Map<String, Object> baseParams = new HashMap<>();
        baseParams.put("fromAccountId", dto.getFromAccountId());
        baseParams.put("toAccountId", dto.getToAccountId());
        baseParams.put("amount", dto.getAmount());
        baseParams.put("currency", dto.getCurrency());
        baseParams.put("channelCode", dto.getChannelCode());
        baseParams.put("businessNo", dto.getBusinessNo());
        baseParams.put("requestId", dto.getRequestId());
        baseParams.put("operator", dto.getOperator());
        baseParams.put("remark", dto.getRemark());

        SagaStep step1 = new SagaStep(
                SnowflakeIdGenerator.nextIdStr(),
                "步骤1-冻结付款方资金",
                1,
                "accountService",
                "freezeBalance",
                "unfreezeBalance"
        );
        step1.setParams(new HashMap<>(baseParams));
        saga.addStep(step1);

        SagaStep step2 = new SagaStep(
                SnowflakeIdGenerator.nextIdStr(),
                "步骤2-渠道出款",
                2,
                "channelService",
                "forwardPayment",
                "compensatePayment"
        );
        step2.setParams(new HashMap<>(baseParams));
        saga.addStep(step2);

        SagaStep step3 = new SagaStep(
                SnowflakeIdGenerator.nextIdStr(),
                "步骤3-清算入账",
                3,
                "clearingService",
                "forwardClearing",
                "compensateClearing"
        );
        step3.setParams(new HashMap<>(baseParams));
        saga.addStep(step3);

        SagaStep step4 = new SagaStep(
                SnowflakeIdGenerator.nextIdStr(),
                "步骤4-解冻扣款",
                4,
                "accountService",
                "unfreezeAndDeduct",
                "rollbackDeduct"
        );
        step4.setParams(new HashMap<>(baseParams));
        saga.addStep(step4);

        String sagaId = sagaCoordinator.executeSaga(saga);
        log.info("Saga跨行转账执行完成, sagaId={}", sagaId);
        return sagaId;
    }

    /**
     * 原路退款 - Saga模式
     * 拆分为多步骤：
     * 1. 渠道退款
     * 2. 清算记账
     * 3. 用户入账
     * 失败时逆向补偿
     */
    @Override
    public String executeRefundWithSaga(String originalPaymentId, String refundAccountId,
                                        BigDecimal amount, String currency,
                                        String businessNo, String operator, String remark) {
        log.info("开始Saga原路退款, originalPaymentId={}, amount={}", originalPaymentId, amount);

        SagaTransaction saga = new SagaTransaction();
        saga.setSagaName("原路退款");
        saga.setBusinessNo(businessNo);
        saga.setTransactionType(DistributedTransactionTypeEnum.SAGA.getCode());

        Map<String, Object> baseParams = new HashMap<>();
        baseParams.put("originalPaymentId", originalPaymentId);
        baseParams.put("refundAccountId", refundAccountId);
        baseParams.put("amount", amount);
        baseParams.put("currency", currency);
        baseParams.put("businessNo", businessNo);
        baseParams.put("operator", operator);
        baseParams.put("remark", remark);

        SagaStep step1 = new SagaStep(
                SnowflakeIdGenerator.nextIdStr(),
                "步骤1-渠道退款",
                1,
                "channelService",
                "forwardRefund",
                "compensateRefund"
        );
        step1.setParams(new HashMap<>(baseParams));
        saga.addStep(step1);

        SagaStep step2 = new SagaStep(
                SnowflakeIdGenerator.nextIdStr(),
                "步骤2-清算记账",
                2,
                "clearingService",
                "forwardRefundClearing",
                "compensateRefundClearing"
        );
        step2.setParams(new HashMap<>(baseParams));
        saga.addStep(step2);

        SagaStep step3 = new SagaStep(
                SnowflakeIdGenerator.nextIdStr(),
                "步骤3-用户入账",
                3,
                "paymentService",
                "forwardUserCredit",
                "compensateUserCredit"
        );
        step3.setParams(new HashMap<>(baseParams));
        saga.addStep(step3);

        String sagaId = sagaCoordinator.executeSaga(saga);
        log.info("Saga原路退款执行完成, sagaId={}", sagaId);
        return sagaId;
    }

    @Override
    public SagaTransaction getSagaTransaction(String sagaId) {
        return sagaCoordinator.getSagaTransaction(sagaId);
    }

    @Override
    public List<SagaTransactionLog> getSagaTransactionLogs(String sagaId) {
        return sagaCoordinator.getSagaLogs(sagaId);
    }

    @Override
    public boolean compensateTransaction(String transactionId, Map<String, Object> params) {
        return transactionCompensator.compensate(transactionId, params);
    }

    @Override
    public boolean retryCompensate(String transactionId) {
        return transactionCompensator.retryCompensate(transactionId);
    }
}
