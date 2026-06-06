package com.bank.core.account.channel;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 渠道通知响应
 * 用于封装外部渠道返回的响应结果
 */
@Data
@Builder
public class ChannelNotificationResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否成功 */
    private boolean success;

    /** 响应码 */
    private String responseCode;

    /** 响应消息 */
    private String responseMessage;

    /** 渠道返回的订单号 */
    private String channelOrderNo;

    /** 渠道处理时间 */
    private LocalDateTime channelTime;

    /** 原始响应数据 */
    private String rawResponse;
}
