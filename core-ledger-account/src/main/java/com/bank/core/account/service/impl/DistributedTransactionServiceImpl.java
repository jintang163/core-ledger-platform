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

    @Override
    @GlobalTransactional(name = "tcc-transfer", rollbackFor = Exception.class)
    public String transferWithTcc(TransferDTO dto) {
        String xid = RootContext.getXID();
        log.info("开始TCC转账, xid={}, businessNo={}", xid, dto.getBusinessNo());

        try {
            BusinessActionContext context = new BusinessActionContext();
            boolean trySuccess = transferTccAction.tryTransfer(context, dto);

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

    @Override
    @GlobalTransactional(name = "tcc-freeze", rollbackFor = Exception.class)
    public String freezeWithTcc(AccountFreezeDTO dto) {
        String xid = RootContext.getXID();
        log.info("开始TCC冻结, xid={}, accountId={}", xid, dto.getAccountId());

        try {
            BusinessActionContext context = new BusinessActionContext();
            boolean trySuccess = freezeTccAction.tryFreeze(context, dto);

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

    @Override
    public String executeCrossBankTransferWithSaga(TransferDTO dto) {
        log.info("开始Saga跨行转账, businessNo={}", dto.getBusinessNo());

        SagaTransaction saga = new SagaTransaction();
        saga.setSagaName("跨行转账");
        saga.setBusinessNo(dto.getBusinessNo());

        Map<String, Object> stepParams = new HashMap<>();
        stepParams.put("fromAccountId", dto.getFromAccountId());
        stepParams.put("toAccountId", dto.getToAccountId());
        stepParams.put("amount", dto.getAmount());
        stepParams.put("currency", dto.getCurrency());
        stepParams.put("channelCode", dto.getChannelCode());
        stepParams.put("businessNo", dto.getBusinessNo());
        stepParams.put("requestId", dto.getRequestId());
        stepParams.put("operator", dto.getOperator());
        stepParams.put("remark", dto.getRemark());

        SagaStep step = new SagaStep(
                SnowflakeIdGenerator.nextIdStr(),
                "跨行转账-渠道处理",
                1,
                "crossBankTransfer",
                "forward",
                "compensate"
        );
        step.setParams(stepParams);
        saga.addStep(step);

        String sagaId = sagaCoordinator.executeSaga(saga);
        log.info("Saga跨行转账执行完成, sagaId={}", sagaId);
        return sagaId;
    }

    @Override
    public String executeRefundWithSaga(String originalPaymentId, String refundAccountId,
                                        BigDecimal amount, String currency,
                                        String businessNo, String operator, String remark) {
        log.info("开始Saga原路退款, originalPaymentId={}, amount={}", originalPaymentId, amount);

        SagaTransaction saga = new SagaTransaction();
        saga.setSagaName("原路退款");
        saga.setBusinessNo(businessNo);

        Map<String, Object> stepParams = new HashMap<>();
        stepParams.put("originalPaymentId", originalPaymentId);
        stepParams.put("refundAccountId", refundAccountId);
        stepParams.put("amount", amount);
        stepParams.put("currency", currency);
        stepParams.put("businessNo", businessNo);
        stepParams.put("operator", operator);
        stepParams.put("remark", remark);

        SagaStep step = new SagaStep(
                SnowflakeIdGenerator.nextIdStr(),
                "原路退款-资金退回",
                1,
                "refund",
                "forward",
                "compensate"
        );
        step.setParams(stepParams);
        saga.addStep(step);

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
