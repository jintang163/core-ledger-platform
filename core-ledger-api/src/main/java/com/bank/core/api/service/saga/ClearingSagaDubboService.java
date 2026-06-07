package com.bank.core.api.service.saga;

import com.bank.core.common.result.Result;

import java.util.Map;

/**
 * 清算服务Saga接口 - 跨服务Dubbo调用
 * 
 * 服务边界：Clearing服务
 * 负责：清算记账、资金清算、对账处理
 */
public interface ClearingSagaDubboService {

    /**
     * Saga正向 - 清算入账
     * 执行跨行清算记账，更新清算账户余额
     */
    Result<Map<String, Object>> forwardClearing(String sagaId, Map<String, Object> params);

    /**
     * Saga补偿 - 冲正清算
     * 冲正已执行的清算记账，恢复清算账户余额
     */
    Result<Boolean> compensateClearing(String sagaId, Map<String, Object> params);

    /**
     * Saga正向 - 清算退款记账
     * 执行退款清算记账，更新相关账户余额
     */
    Result<Map<String, Object>> forwardRefundClearing(String sagaId, Map<String, Object> params);

    /**
     * Saga补偿 - 冲正退款清算
     * 冲正已执行的退款清算记账
     */
    Result<Boolean> compensateRefundClearing(String sagaId, Map<String, Object> params);

    String getServiceName();
}
