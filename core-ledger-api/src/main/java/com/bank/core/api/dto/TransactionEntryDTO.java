package com.bank.core.api.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class TransactionEntryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "账户ID不能为空")
    private String accountId;

    @NotBlank(message = "科目代码不能为空")
    private String subjectCode;

    @NotBlank(message = "科目名称不能为空")
    private String subjectName;

    @NotNull(message = "借贷方向不能为空")
    private Integer direction;

    @NotNull(message = "金额不能为空")
    @Positive(message = "金额必须大于0")
    private BigDecimal amount;

    private String summary;
}
