package com.bank.core.api.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 充值请求DTO
 * 用于单账户充值（入金）操作的请求参数
 * 资金流向：外部渠道 → 内部用户账户
 */
@Data
@ApiModel(value = "充值请求", description = "单账户充值（入金）请求参数")
public class RechargeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 请求ID（用于幂等校验，每次请求唯一） */
    @NotBlank(message = "请求ID不能为空")
    @ApiModelProperty(value = "请求ID", required = true, example = "REQ2024010100001",
            notes = "用于幂等校验，每次请求唯一")
    private String requestId;

    /** 业务流水号（调用方业务系统的唯一流水号，用于幂等） */
    @NotBlank(message = "业务流水号不能为空")
    @ApiModelProperty(value = "业务流水号", required = true, example = "BIZ2024010100001",
            notes = "调用方业务系统的唯一流水号，用于幂等校验")
    private String businessNo;

    /** 账户ID（需要充值的内部账户ID） */
    @NotBlank(message = "账户ID不能为空")
    @ApiModelProperty(value = "账户ID", required = true, example = "ACC1000000001",
            notes = "需要充值的内部账户ID")
    private String accountId;

    /** 充值金额（元，必须大于0） */
    @NotNull(message = "充值金额不能为空")
    @Positive(message = "充值金额必须大于0")
    @ApiModelProperty(value = "充值金额", required = true, example = "1000.00",
            notes = "充值金额，单位：元，必须大于0")
    private BigDecimal amount;

    /** 币种（如：CNY, USD, EUR等） */
    @NotBlank(message = "币种不能为空")
    @ApiModelProperty(value = "币种", required = true, example = "CNY",
            notes = "币种编码，如：CNY（人民币）、USD（美元）等")
    private String currency;

    /** 渠道编码（ALIPAY-支付宝, WECHAT-微信支付, UNIONPAY-银联等） */
    @NotBlank(message = "渠道编码不能为空")
    @ApiModelProperty(value = "渠道编码", required = true, example = "ALIPAY",
            notes = "支付渠道编码：ALIPAY（支付宝）、WECHAT（微信支付）、UNIONPAY（银联）、MOCK（模拟渠道）")
    private String channelCode;

    /** 渠道订单号（外部渠道的订单号，可选） */
    @ApiModelProperty(value = "渠道订单号", example = "ALIPAY2024010100001",
            notes = "外部渠道的订单号，可选")
    private String channelOrderNo;

    /** 回调地址（渠道处理完成后的回调通知地址） */
    @ApiModelProperty(value = "回调地址", example = "https://api.example.com/callback/recharge",
            notes = "渠道处理完成后的回调通知地址")
    private String callbackUrl;

    /** 备注 */
    @ApiModelProperty(value = "备注", example = "用户充值",
            notes = "业务备注信息")
    private String remark;

    /** 操作员 */
    @ApiModelProperty(value = "操作员", example = "admin",
            notes = "操作人标识")
    private String operator;
}
