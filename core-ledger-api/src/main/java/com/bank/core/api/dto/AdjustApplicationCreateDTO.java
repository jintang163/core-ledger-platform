package com.bank.core.api.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class AdjustApplicationCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "账户ID不能为空")
    private String accountId;

    @NotNull(message = "调账类型不能为空")
    private Integer adjustType;

    @NotNull(message = "调账金额不能为空")
    private BigDecimal amount;

    @NotBlank(message = "调账原因不能为空")
    private String reason;

    private String remark;

    @NotBlank(message = "请求ID不能为空")
    private String requestId;
}
