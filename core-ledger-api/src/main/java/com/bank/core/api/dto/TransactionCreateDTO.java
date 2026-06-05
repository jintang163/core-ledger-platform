package com.bank.core.api.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class TransactionCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "请求ID不能为空")
    private String requestId;

    @NotBlank(message = "业务流水号不能为空")
    private String businessNo;

    @NotNull(message = "交易类型不能为空")
    private Integer transactionType;

    @NotBlank(message = "币种不能为空")
    private String currency;

    @NotNull(message = "交易金额不能为空")
    private BigDecimal totalAmount;

    private String summary;

    private String operator;

    @NotEmpty(message = "分录不能为空")
    @Valid
    private List<TransactionEntryDTO> entries;
}
