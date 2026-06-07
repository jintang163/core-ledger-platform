package com.bank.core.api.service.saga;

import com.bank.core.common.result.Result;

import java.util.Map;

/**
 * 支付服务Saga接口 - 跨服务Dubbo调用
 * 
 * 服务边界：Payment服务
 * 负责：支付订单管理、用户账户入账
 */
public interface PaymentSagaDubboService {

    /**
     * Saga正向 - 用户账户入账
     * 跨行转账成功后给收款方用户账户入账
     */
    Result<Map<String, Object>> forwardUserCredit(String sagaId, Map<String, Object> params);

    /**
     * Saga补偿 - 用户账户扣款
     * 跨行转账失败时冲正已入账的金额
     */
    Result<Boolean> compensateUserCredit(String sagaId, Map<String, Object> params);

    /**
     * Saga正向 - 更新支付订单状态
     * 更新支付订单为处理中/成功/失败
     */
    Result<Map<String, Object>> forwardUpdateOrder(String sagaId, Map<String, Object> params);

    /**
     * Saga补偿 - 回滚支付订单状态
     * 回滚支付订单到前一状态
     */
    Result<Boolean> compensateUpdateOrder(String sagaId, Map<String, Object> params);

    String getServiceName();
}
