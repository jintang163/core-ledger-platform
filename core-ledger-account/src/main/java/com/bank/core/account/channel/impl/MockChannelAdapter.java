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
 * 模拟渠道适配器
 * 用于测试环境，模拟外部渠道的响应
 * 所有请求默认返回成功，便于开发和测试
 */
@Slf4j
@Component
public class MockChannelAdapter implements ChannelAdapter {

    @Override
    public String getChannelCode() {
        return ChannelCodeEnum.MOCK.getCode();
    }

    /**
     * 模拟充值通知
     * 打印请求日志，直接返回成功响应
     * @param request 通知请求
     * @return 成功响应
     */
    @Override
    public ChannelNotificationResponse notifyRecharge(ChannelNotificationRequest request) {
        log.info("【模拟渠道】接收充值通知请求, request: {}", JSON.toJSONString(request));

        // 模拟渠道处理延迟
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ChannelNotificationResponse response = ChannelNotificationResponse.builder()
                .success(true)
                .responseCode("MOCK_0000")
                .responseMessage("模拟渠道充值通知成功")
                .channelOrderNo("MOCK" + System.currentTimeMillis())
                .channelTime(LocalDateTime.now())
                .rawResponse(JSON.toJSONString(request))
                .build();

        log.info("【模拟渠道】充值通知响应, response: {}", JSON.toJSONString(response));
        return response;
    }

    /**
     * 模拟提现通知
     * 打印请求日志，直接返回成功响应
     * @param request 通知请求
     * @return 成功响应
     */
    @Override
    public ChannelNotificationResponse notifyWithdraw(ChannelNotificationRequest request) {
        log.info("【模拟渠道】接收提现通知请求, request: {}", JSON.toJSONString(request));

        // 模拟渠道处理延迟
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ChannelNotificationResponse response = ChannelNotificationResponse.builder()
                .success(true)
                .responseCode("MOCK_0000")
                .responseMessage("模拟渠道提现通知成功")
                .channelOrderNo("MOCK" + System.currentTimeMillis())
                .channelTime(LocalDateTime.now())
                .rawResponse(JSON.toJSONString(request))
                .build();

        log.info("【模拟渠道】提现通知响应, response: {}", JSON.toJSONString(response));
        return response;
    }

    /**
     * 模拟查询订单状态
     * @param channelCode 渠道编码
     * @param channelOrderNo 渠道订单号
     * @return 成功响应
     */
    @Override
    public ChannelNotificationResponse queryOrderStatus(String channelCode, String channelOrderNo) {
        log.info("【模拟渠道】查询订单状态, channelCode: {}, channelOrderNo: {}", channelCode, channelOrderNo);

        return ChannelNotificationResponse.builder()
                .success(true)
                .responseCode("MOCK_0000")
                .responseMessage("模拟渠道订单状态查询成功")
                .channelOrderNo(channelOrderNo)
                .channelTime(LocalDateTime.now())
                .build();
    }

    @Override
    public ChannelNotificationResponse notifyCrossBankTransfer(ChannelNotificationRequest request) {
        log.info("【模拟渠道】接收跨行转账通知请求, request: {}", JSON.toJSONString(request));

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ChannelNotificationResponse response = ChannelNotificationResponse.builder()
                .success(true)
                .responseCode("MOCK_0000")
                .responseMessage("模拟渠道跨行转账通知成功")
                .channelOrderNo("MOCK_CROSS_" + System.currentTimeMillis())
                .channelTime(LocalDateTime.now())
                .rawResponse(JSON.toJSONString(request))
                .build();

        log.info("【模拟渠道】跨行转账通知响应, response: {}", JSON.toJSONString(response));
        return response;
    }

    @Override
    public boolean cancelTransaction(java.util.Map<String, Object> params) {
        log.info("【模拟渠道】取消交易, params: {}", JSON.toJSONString(params));

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("【模拟渠道】取消交易成功");
        return true;
    }
}
