package com.bank.core.api.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class AccountCloseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "账户ID不能为空")
    private String accountId;

    private String remark;

    private String operator;

    private String requestId;
}
