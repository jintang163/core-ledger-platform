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
 * 账户间转账请求DTO
 * 用于内部账户之间的转账操作
 * 资金流向：内部账户A（扣款方） → 内部账户B（收款方）
 */
@Data
@ApiModel(value = "账户间转账请求", description = "内部账户之间的转账操作参数")
public class TransferDTO implements Serializable {

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

    /** 付款方账户ID（扣款方） */
    @NotBlank(message = "付款方账户ID不能为空")
    @ApiModelProperty(value = "付款方账户ID", required = true, example = "ACC1000000001",
            notes = "扣款方账户ID")
    private String fromAccountId;

    /** 收款方账户ID */
    @NotBlank(message = "收款方账户ID不能为空")
    @ApiModelProperty(value = "收款方账户ID", required = true, example = "ACC1000000002",
            notes = "收款方账户ID")
    private String toAccountId;

    /** 转账金额（元，必须大于0） */
    @NotNull(message = "转账金额不能为空")
    @Positive(message = "转账金额必须大于0")
    @ApiModelProperty(value = "转账金额", required = true, example = "1000.00",
            notes = "转账金额，单位：元，必须大于0")
    private BigDecimal amount;

    /** 币种（如：CNY, USD, EUR等） */
    @NotBlank(message = "币种不能为空")
    @ApiModelProperty(value = "币种", required = true, example = "CNY",
            notes = "币种编码，如：CNY（人民币）、USD（美元）等")
    private String currency;

    /** 备注 */
    @ApiModelProperty(value = "备注", example = "货款转账",
            notes = "业务备注信息")
    private String remark;

    /** 渠道编码（跨行转账时使用） */
    @ApiModelProperty(value = "渠道编码", example = "ALIPAY",
            notes = "跨行转账时使用的渠道编码")
    private String channelCode;

    /** 转账类型：1-内部转账 2-跨行转账 */
    @ApiModelProperty(value = "转账类型", example = "1",
            notes = "1-内部转账 2-跨行转账")
    private Integer transferType;

    /** 分布式事务类型：1-TCC 2-Saga */
    @ApiModelProperty(value = "分布式事务类型", example = "1",
            notes = "1-TCC 2-Saga")
    private Integer distributedTxType;

    /** 操作员 */
    @ApiModelProperty(value = "操作员", example = "admin",
            notes = "操作人标识")
    private String operator;

    /** 回调URL（交易成功后异步通知） */
    @ApiModelProperty(value = "回调URL", example = "https://api.example.com/callback/transfer",
            notes = "交易成功后异步通知的回调地址")
    private String callbackUrl;
}
