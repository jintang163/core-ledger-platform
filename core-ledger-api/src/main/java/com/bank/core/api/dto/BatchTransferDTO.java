package com.bank.core.api.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
public class BatchTransferDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "请求ID不能为空")
    private String requestId;

    @NotBlank(message = "业务流水号不能为空")
    private String businessNo;

    @NotBlank(message = "付款方账户ID不能为空")
    private String fromAccountId;

    @NotBlank(message = "币种不能为空")
    private String currency;

    @NotEmpty(message = "转账明细不能为空")
    @Valid
    private List<BatchTransferItemDTO> items;

    private String remark;

    private String operator;
}
