package com.bank.core.api.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class AccountUnfreezeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "账户ID不能为空")
    private String accountId;

    @NotNull(message = "冻结类型不能为空")
    private Integer freezeType;

    private String remark;

    private String operator;

    private String requestId;
}
