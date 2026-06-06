package com.bank.core.api.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 转账订单VO
 * 用于账户间转账订单的返回结果
 */
@Data
@ApiModel(value = "转账订单", description = "账户间转账订单信息")
public class TransferOrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 转账订单ID（系统内部唯一标识） */
    @ApiModelProperty(value = "转账订单ID", example = "TRF2024010100001",
            notes = "系统内部唯一标识")
    private String transferId;

    /** 转账单号（业务可见的单号） */
    @ApiModelProperty(value = "转账单号", example = "TRFNO2024010100001",
            notes = "业务可见的单号")
    private String transferNo;

    /** 业务流水号（调用方传入的唯一标识） */
    @ApiModelProperty(value = "业务流水号", example = "BIZ2024010100001",
            notes = "调用方传入的唯一标识")
    private String businessNo;

    /** 请求ID（用于幂等校验） */
    @ApiModelProperty(value = "请求ID", example = "REQ2024010100001",
            notes = "用于幂等校验")
    private String requestId;

    /** 付款方账户ID */
    @ApiModelProperty(value = "付款方账户ID", example = "ACC1000000001")
    private String fromAccountId;

    /** 付款方账户号 */
    @ApiModelProperty(value = "付款方账户号", example = "6222021234567890123")
    private String fromAccountNo;

    /** 收款方账户ID */
    @ApiModelProperty(value = "收款方账户ID", example = "ACC1000000002")
    private String toAccountId;

    /** 收款方账户号 */
    @ApiModelProperty(value = "收款方账户号", example = "6222029876543210987")
    private String toAccountNo;

    /** 转账金额（元） */
    @ApiModelProperty(value = "转账金额", example = "1000.00",
            notes = "单位：元")
    private BigDecimal amount;

    /** 币种 */
    @ApiModelProperty(value = "币种", example = "CNY")
    private String currency;

    /** 订单状态（0-待处理, 1-处理中, 2-成功, 3-失败） */
    @ApiModelProperty(value = "订单状态", example = "2",
            notes = "0-待处理, 1-处理中, 2-成功, 3-失败")
    private Integer status;

    /** 订单状态描述 */
    @ApiModelProperty(value = "订单状态描述", example = "成功")
    private String statusDesc;

    /** 关联的会计交易ID */
    @ApiModelProperty(value = "会计交易ID", example = "TX2024010100001")
    private String transactionId;

    /** 备注 */
    @ApiModelProperty(value = "备注", example = "货款转账")
    private String remark;

    /** 操作员 */
    @ApiModelProperty(value = "操作员", example = "admin")
    private String operator;

    /** 转账时间 */
    @ApiModelProperty(value = "转账时间")
    private LocalDateTime transferTime;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}
