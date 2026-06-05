package com.bank.core.api.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class AccountCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @NotNull(message = "账户类型不能为空")
    private Integer accountType;

    @NotBlank(message = "币种不能为空")
    private String currency;

    @NotNull(message = "初始余额不能为空")
    @PositiveOrZero(message = "初始余额不能为负数")
    private BigDecimal initBalance;

    private String requestId;
}
