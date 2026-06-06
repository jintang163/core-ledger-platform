package com.bank.core.account.channel;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 渠道通知请求
 * 用于封装发送给外部渠道的通知请求参数
 */
@Data
@Builder
public class ChannelNotificationRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 支付订单ID */
    private String paymentId;

    /** 支付单号 */
    private String paymentNo;

    /** 业务流水号 */
    private String businessNo;

    /** 渠道编码 */
    private String channelCode;

    /** 渠道订单号 */
    private String channelOrderNo;

    /** 支付类型：1-充值 2-提现 */
    private Integer paymentType;

    /** 支付金额 */
    private BigDecimal amount;

    /** 币种 */
    private String currency;

    /** 账户ID */
    private String accountId;

    /** 账户号 */
    private String accountNo;

    /** 通知状态：PENDING-待通知 CHANNEL_SUCCESS-渠道成功 CHANNEL_FAILED-渠道失败 */
    private String channelStatus;

    /** 回调地址 */
    private String callbackUrl;

    /** 备注 */
    private String remark;

    /** 操作员 */
    private String operator;

    /** 请求时间 */
    private LocalDateTime requestTime;
}
