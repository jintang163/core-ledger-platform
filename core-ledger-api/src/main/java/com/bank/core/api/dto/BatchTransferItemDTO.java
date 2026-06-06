package com.bank.core.api.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 批量转账明细DTO
 * 批量转账中的单条转账记录
 */
@Data
@ApiModel(value = "批量转账明细", description = "批量转账中的单条收款记录")
public class BatchTransferItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 收款方账户ID */
    @NotBlank(message = "收款方账户ID不能为空")
    @ApiModelProperty(value = "收款方账户ID", required = true, example = "ACC1000000002",
            notes = "收款方账户ID")
    private String toAccountId;

    /** 转账金额（元，必须大于0） */
    @NotNull(message = "转账金额不能为空")
    @Positive(message = "转账金额必须大于0")
    @ApiModelProperty(value = "转账金额", required = true, example = "1000.00",
            notes = "转账金额，单位：元，必须大于0")
    private BigDecimal amount;

    /** 备注 */
    @ApiModelProperty(value = "备注", example = "1月工资",
            notes = "本条明细的备注信息")
    private String remark;
}
