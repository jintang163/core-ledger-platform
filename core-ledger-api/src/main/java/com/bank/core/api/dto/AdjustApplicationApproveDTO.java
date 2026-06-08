package com.bank.core.api.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class AdjustApplicationApproveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "申请单ID不能为空")
    private String id;

    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    private String approveRemark;

    @NotBlank(message = "请求ID不能为空")
    private String requestId;
}
