package com.bank.core.api.service.saga;

import com.bank.core.common.result.Result;

import java.util.Map;

/**
 * 渠道服务Saga接口 - 跨服务Dubbo调用
 * 
 * 服务边界：Channel服务
 * 负责：与外部渠道交互（出款、退款、取消交易）
 */
public interface ChannelSagaDubboService {

    /**
     * Saga正向 - 渠道出款
     * 调用外部渠道接口执行跨行转账出款
     */
    Result<Map<String, Object>> forwardPayment(String sagaId, Map<String, Object> params);

    /**
     * Saga补偿 - 取消渠道出款
     * 调用外部渠道接口取消已发起的出款交易
     */
    Result<Boolean> compensatePayment(String sagaId, Map<String, Object> params);

    /**
     * Saga正向 - 渠道退款
     * 调用外部渠道接口执行原路退款
     */
    Result<Map<String, Object>> forwardRefund(String sagaId, Map<String, Object> params);

    /**
     * Saga补偿 - 取消渠道退款
     * 调用外部渠道接口取消已发起的退款交易
     */
    Result<Boolean> compensateRefund(String sagaId, Map<String, Object> params);

    String getServiceName();
}
