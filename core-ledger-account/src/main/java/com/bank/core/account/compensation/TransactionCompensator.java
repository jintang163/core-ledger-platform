package com.bank.core.account.compensation;

import com.bank.core.account.entity.SagaTransactionLog;

import java.util.List;
import java.util.Map;

public interface TransactionCompensator {

    boolean compensate(String transactionId, Map<String, Object> params);

    boolean compensate(SagaTransactionLog log);

    List<SagaTransactionLog> findFailedTransactions(int hours);

    boolean retryCompensate(String transactionId);

    void autoCompensateFailedTransactions();
}
