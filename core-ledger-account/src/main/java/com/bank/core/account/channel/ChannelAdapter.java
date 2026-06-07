package com.bank.core.account.channel;

/**
 * 渠道适配器接口
 * 定义与外部渠道交互的统一接口，支持多种渠道实现
 *
 * 设计模式：适配器模式 + 策略模式
 * 不同渠道（支付宝、微信、银联等）实现各自的适配器
 */
public interface ChannelAdapter {

    /**
     * 获取渠道编码
     * @return 渠道编码
     */
    String getChannelCode();

    /**
     * 通知渠道-充值请求
     * 通知外部渠道有充值请求，等待渠道确认
     * @param request 通知请求
     * @return 通知响应
     */
    ChannelNotificationResponse notifyRecharge(ChannelNotificationRequest request);

    /**
     * 通知渠道-提现请求
     * 通知外部渠道有提现请求，等待渠道处理
     * @param request 通知请求
     * @return 通知响应
     */
    ChannelNotificationResponse notifyWithdraw(ChannelNotificationRequest request);

    /**
     * 查询渠道订单状态
     * @param channelCode 渠道编码
     * @param channelOrderNo 渠道订单号
     * @return 通知响应
     */
    ChannelNotificationResponse queryOrderStatus(String channelCode, String channelOrderNo);

    /**
     * 通知渠道-跨行转账请求
     * @param request 通知请求
     * @return 通知响应
     */
    ChannelNotificationResponse notifyCrossBankTransfer(ChannelNotificationRequest request);

    /**
     * 取消渠道交易
     * @param params 取消参数
     * @return 是否取消成功
     */
    boolean cancelTransaction(java.util.Map<String, Object> params);
}
