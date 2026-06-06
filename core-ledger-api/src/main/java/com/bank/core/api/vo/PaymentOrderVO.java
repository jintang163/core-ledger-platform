package com.bank.core.api.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单VO
 * 用于充值（入金）和提现（出金）订单的返回结果
 */
@Data
@ApiModel(value = "支付订单", description = "充值/提现订单信息")
public class PaymentOrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 支付订单ID（系统内部唯一标识） */
    @ApiModelProperty(value = "支付订单ID", example = "PAY2024010100001",
            notes = "系统内部唯一标识")
    private String paymentId;

    /** 支付单号（业务可见的单号） */
    @ApiModelProperty(value = "支付单号", example = "PAYNO2024010100001",
            notes = "业务可见的单号")
    private String paymentNo;

    /** 业务流水号（调用方传入的唯一标识） */
    @ApiModelProperty(value = "业务流水号", example = "BIZ2024010100001",
            notes = "调用方传入的唯一标识")
    private String businessNo;

    /** 请求ID（用于幂等校验） */
    @ApiModelProperty(value = "请求ID", example = "REQ2024010100001",
            notes = "用于幂等校验")
    private String requestId;

    /** 支付类型（1-充值, 2-提现） */
    @ApiModelProperty(value = "支付类型", example = "1",
            notes = "1-充值（入金）, 2-提现（出金）")
    private Integer paymentType;

    /** 支付类型描述 */
    @ApiModelProperty(value = "支付类型描述", example = "充值")
    private String paymentTypeDesc;

    /** 账户ID */
    @ApiModelProperty(value = "账户ID", example = "ACC1000000001")
    private String accountId;

    /** 账户号 */
    @ApiModelProperty(value = "账户号", example = "6222021234567890123")
    private String accountNo;

    /** 金额（元） */
    @ApiModelProperty(value = "金额", example = "1000.00",
            notes = "单位：元")
    private BigDecimal amount;

    /** 币种 */
    @ApiModelProperty(value = "币种", example = "CNY")
    private String currency;

    /** 渠道编码（ALIPAY, WECHAT, UNIONPAY, MOCK等） */
    @ApiModelProperty(value = "渠道编码", example = "ALIPAY",
            notes = "ALIPAY-支付宝, WECHAT-微信支付, UNIONPAY-银联, MOCK-模拟渠道")
    private String channelCode;

    /** 渠道订单号 */
    @ApiModelProperty(value = "渠道订单号", example = "ALIPAY2024010100001")
    private String channelOrderNo;

    /** 订单状态（0-待处理, 1-处理中, 2-成功, 3-失败） */
    @ApiModelProperty(value = "订单状态", example = "2",
            notes = "0-待处理, 1-处理中, 2-成功, 3-失败")
    private Integer status;

    /** 订单状态描述 */
    @ApiModelProperty(value = "订单状态描述", example = "成功")
    private String statusDesc;

    /** 渠道状态（0-待通知, 1-通知成功, 2-通知失败） */
    @ApiModelProperty(value = "渠道状态", example = "1",
            notes = "0-待通知, 1-通知成功, 2-通知失败")
    private Integer channelStatus;

    /** 渠道状态描述 */
    @ApiModelProperty(value = "渠道状态描述", example = "通知成功")
    private String channelStatusDesc;

    /** 备注 */
    @ApiModelProperty(value = "备注", example = "用户充值")
    private String remark;

    /** 操作员 */
    @ApiModelProperty(value = "操作员", example = "admin")
    private String operator;

    /** 渠道处理时间 */
    @ApiModelProperty(value = "渠道处理时间")
    private LocalDateTime channelTime;

    /** 成功时间 */
    @ApiModelProperty(value = "成功时间")
    private LocalDateTime successTime;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}
