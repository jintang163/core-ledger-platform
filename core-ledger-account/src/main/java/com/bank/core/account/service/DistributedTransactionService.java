package com.bank.core.account.service;

import com.bank.core.account.entity.SagaTransactionLog;
import com.bank.core.account.saga.SagaTransaction;
import com.bank.core.api.dto.AccountFreezeDTO;
import com.bank.core.api.dto.TransferDTO;

import java.util.List;
import java.util.Map;

public interface DistributedTransactionService {

    String transferWithTcc(TransferDTO dto);

    String freezeWithTcc(AccountFreezeDTO dto);

    String executeCrossBankTransferWithSaga(TransferDTO dto);

    String executeRefundWithSaga(String originalPaymentId, String refundAccountId,
                                   java.math.BigDecimal amount, String currency,
                                   String businessNo, String operator, String remark);

    SagaTransaction getSagaTransaction(String sagaId);

    List<SagaTransactionLog> getSagaTransactionLogs(String sagaId);

    boolean compensateTransaction(String transactionId, Map<String, Object> params);

    boolean retryCompensate(String transactionId);
}
