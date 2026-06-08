package com.bank.core.api.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class AdjustApplicationExecuteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "申请单ID不能为空")
    private String id;

    @NotBlank(message = "请求ID不能为空")
    private String requestId;
}
