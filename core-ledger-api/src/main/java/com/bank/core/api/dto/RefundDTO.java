package com.bank.core.api.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 退款请求DTO
 * 用于原路退款请求
 */
@Data
@ApiModel(value = "退款请求", description = "原路退款请求参数")
public class RefundDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 原支付订单ID */
    @NotBlank(message = "原支付订单ID不能为空")
    @ApiModelProperty(value = "原支付订单ID", example = "PAY2024010100001", required = true,
            notes = "需要退款的原支付订单ID")
    private String originalPaymentId;

    /** 退款账户ID（退款到哪个账户） */
    @NotBlank(message = "退款账户ID不能为空")
    @ApiModelProperty(value = "退款账户ID", example = "ACC1000000001", required = true,
            notes = "退款资金入账的账户ID")
    private String refundAccountId;

    /** 退款金额（元） */
    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须大于0")
    @ApiModelProperty(value = "退款金额（元）", example = "1000.00", required = true,
            notes = "退款金额，不能大于原支付金额")
    private BigDecimal amount;

    /** 币种 */
    @NotBlank(message = "币种不能为空")
    @ApiModelProperty(value = "币种", example = "CNY", required = true,
            notes = "退款币种，必须与原支付币种一致")
    private String currency;

    /** 业务流水号（调用方传入，用于幂等） */
    @NotBlank(message = "业务流水号不能为空")
    @ApiModelProperty(value = "业务流水号", example = "REFUND2024010100001", required = true,
            notes = "调用方传入的唯一标识，用于幂等校验")
    private String businessNo;

    /** 请求ID（用于幂等校验） */
    @ApiModelProperty(value = "请求ID", example = "REQ2024010100001",
            notes = "用于幂等校验，可不传")
    private String requestId;

    /** 操作员 */
    @ApiModelProperty(value = "操作员", example = "admin",
            notes = "操作人标识")
    private String operator;

    /** 退款原因/备注 */
    @ApiModelProperty(value = "退款原因/备注", example = "用户申请退款",
            notes = "退款的原因说明")
    private String remark;
}
