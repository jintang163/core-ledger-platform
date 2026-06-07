package com.bank.core.account.service;

import com.bank.core.account.entity.TransferOrder;
import com.bank.core.api.dto.TransferDTO;
import com.bank.core.api.vo.PaymentOrderVO;
import com.bank.core.api.vo.TransferOrderVO;

import java.math.BigDecimal;
import java.util.Map;

public interface MessageProducerService {

    void sendAccountChangeMessage(String accountId, String accountNo, String transactionId,
                                   Integer transactionType, BigDecimal amount, String currency,
                                   String requestId, String operator, String businessNo);

    void sendTransferSuccessMessage(TransferOrderVO order);

    void sendPaymentSuccessMessage(PaymentOrderVO order);

    void sendRefundSuccessMessage(PaymentOrderVO order);

    void sendClearingResultMessage(String clearingId, String transactionId,
                                    String accountId, BigDecimal amount, String currency,
                                    String status, String businessNo);

    void sendCallbackNotify(String callbackType, String callbackUrl,
                            Map<String, Object> callbackData, String businessNo);

    void sendTransferSuccessWithCallback(TransferOrderVO order,
                                           String callbackUrl, Map<String, Object> additionalData);

    void sendPaymentSuccessWithCallback(PaymentOrderVO order,
                                          String callbackUrl, Map<String, Object> additionalData);
}
