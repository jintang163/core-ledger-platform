package com.bank.core.account.channel.impl;

import com.alibaba.fastjson.JSON;
import com.bank.core.account.channel.ChannelAdapter;
import com.bank.core.account.channel.ChannelNotificationRequest;
import com.bank.core.account.channel.ChannelNotificationResponse;
import com.bank.core.common.enums.ChannelCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 支付宝渠道适配器
 * 实现与支付宝支付渠道的交互
 *
 * 注意：实际项目中需要接入支付宝SDK，这里提供框架实现
 */
@Slf4j
@Component
public class AlipayChannelAdapter implements ChannelAdapter {

    @Override
    public String getChannelCode() {
        return ChannelCodeEnum.ALIPAY.getCode();
    }

    /**
     * 通知支付宝-充值请求
     * 调用支付宝接口发起充值/支付请求
     * @param request 通知请求
     * @return 支付宝响应
     */
    @Override
    public ChannelNotificationResponse notifyRecharge(ChannelNotificationRequest request) {
        log.info("【支付宝渠道】接收充值通知请求, paymentId: {}, amount: {}",
                request.getPaymentId(), request.getAmount());

        // TODO: 实际项目中调用支付宝SDK alipay.trade.pay
        // AlipayTradePayRequest alipayRequest = new AlipayTradePayRequest();
        // alipayRequest.setBizContent(JSON.toJSONString(buildAlipayBizContent(request)));
        // AlipayTradePayResponse alipayResponse = alipayClient.execute(alipayRequest);

        // 模拟调用
        log.warn("【支付宝渠道】当前为模拟实现，请接入真实支付宝SDK");

        return ChannelNotificationResponse.builder()
                .success(true)
                .responseCode("ALIPAY_0000")
                .responseMessage("支付宝充值通知受理成功")
                .channelOrderNo("ALIPAY" + System.currentTimeMillis())
                .channelTime(LocalDateTime.now())
                .rawResponse(JSON.toJSONString(request))
                .build();
    }

    /**
     * 通知支付宝-提现请求
     * 调用支付宝接口发起提现/转账请求
     * @param request 通知请求
     * @return 支付宝响应
     */
    @Override
    public ChannelNotificationResponse notifyWithdraw(ChannelNotificationRequest request) {
        log.info("【支付宝渠道】接收提现通知请求, paymentId: {}, amount: {}",
                request.getPaymentId(), request.getAmount());

        // TODO: 实际项目中调用支付宝SDK alipay.fund.trans.toaccount
        // AlipayFundTransToaccountTransferRequest transferRequest = ...
        // transferRequest.setBizContent(JSON.toJSONString(buildAlipayTransferContent(request)));
        // AlipayFundTransToaccountTransferResponse transferResponse = alipayClient.execute(transferRequest);

        log.warn("【支付宝渠道】当前为模拟实现，请接入真实支付宝SDK");

        return ChannelNotificationResponse.builder()
                .success(true)
                .responseCode("ALIPAY_0000")
                .responseMessage("支付宝提现通知受理成功")
                .channelOrderNo("ALIPAY" + System.currentTimeMillis())
                .channelTime(LocalDateTime.now())
                .rawResponse(JSON.toJSONString(request))
                .build();
    }

    /**
     * 查询支付宝订单状态
     * @param channelCode 渠道编码
     * @param channelOrderNo 渠道订单号
     * @return 支付宝响应
     */
    @Override
    public ChannelNotificationResponse queryOrderStatus(String channelCode, String channelOrderNo) {
        log.info("【支付宝渠道】查询订单状态, channelOrderNo: {}", channelOrderNo);

        // TODO: 实际项目中调用支付宝SDK alipay.trade.query

        return ChannelNotificationResponse.builder()
                .success(true)
                .responseCode("ALIPAY_0000")
                .responseMessage("支付宝订单状态查询成功")
                .channelOrderNo(channelOrderNo)
                .channelTime(LocalDateTime.now())
                .build();
    }
}
