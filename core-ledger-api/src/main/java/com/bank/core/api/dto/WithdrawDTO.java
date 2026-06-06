package com.bank.core.api.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class WithdrawDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "请求ID不能为空")
    private String requestId;

    @NotBlank(message = "业务流水号不能为空")
    private String businessNo;

    @NotBlank(message = "账户ID不能为空")
    private String accountId;

    @NotNull(message = "提现金额不能为空")
    @Positive(message = "提现金额必须大于0")
    private BigDecimal amount;

    @NotBlank(message = "币种不能为空")
    private String currency;

    @NotBlank(message = "渠道编码不能为空")
    private String channelCode;

    private String channelOrderNo;

    private String callbackUrl;

    private String remark;

    private String operator;
}
