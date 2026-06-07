package com.bank.core.account.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bank.core.api.dto.PaymentCallbackDTO;
import com.bank.core.api.dto.PaymentQueryDTO;
import com.bank.core.api.dto.RechargeDTO;
import com.bank.core.api.dto.WithdrawDTO;
import com.bank.core.api.vo.PaymentOrderVO;

public interface PaymentService {

    PaymentOrderVO recharge(RechargeDTO dto);

    PaymentOrderVO withdraw(WithdrawDTO dto);

    PaymentOrderVO handleCallback(PaymentCallbackDTO dto);

    PaymentOrderVO getPaymentOrder(String paymentId);

    PaymentOrderVO getPaymentOrderByBusinessNo(String businessNo);

    Page<PaymentOrderVO> queryPaymentOrders(PaymentQueryDTO dto);

    PaymentOrderVO refund(String originalPaymentId, String refundAccountId,
                           java.math.BigDecimal amount, String currency,
                           String businessNo, String operator, String remark);
}
