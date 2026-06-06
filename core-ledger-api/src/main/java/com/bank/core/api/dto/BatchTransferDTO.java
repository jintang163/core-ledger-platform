package com.bank.core.api.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

/**
 * 批量转账请求DTO
 * 用于批量转账（代发）操作，一次请求包含多条转账记录
 * 资金流向：内部账户A（扣款方） → 多个内部账户B（收款方）
 *
 * 业务特点：
 * - 支持部分成功，单条失败不影响其他转账
 * - 最多支持1000条明细
 * - 所有明细使用相同的扣款方账户和币种
 */
@Data
@ApiModel(value = "批量转账请求", description = "批量转账（代发）操作参数")
public class BatchTransferDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 请求ID（用于幂等校验，每次请求唯一） */
    @NotBlank(message = "请求ID不能为空")
    @ApiModelProperty(value = "请求ID", required = true, example = "REQ2024010100001",
            notes = "用于幂等校验，每次请求唯一")
    private String requestId;

    /** 业务流水号（调用方业务系统的唯一流水号，用于幂等） */
    @NotBlank(message = "业务流水号不能为空")
    @ApiModelProperty(value = "业务流水号", required = true, example = "BIZ2024010100001",
            notes = "调用方业务系统的唯一流水号，用于幂等校验")
    private String businessNo;

    /** 付款方账户ID（扣款方，所有明细的共同扣款账户） */
    @NotBlank(message = "付款方账户ID不能为空")
    @ApiModelProperty(value = "付款方账户ID", required = true, example = "ACC1000000001",
            notes = "扣款方账户ID，所有明细的共同扣款账户")
    private String fromAccountId;

    /** 币种（如：CNY, USD, EUR等） */
    @NotBlank(message = "币种不能为空")
    @ApiModelProperty(value = "币种", required = true, example = "CNY",
            notes = "币种编码，如：CNY（人民币）、USD（美元）等")
    private String currency;

    /** 转账明细列表（包含多条转账记录，最多1000条） */
    @NotEmpty(message = "转账明细不能为空")
    @Valid
    @ApiModelProperty(value = "转账明细列表", required = true,
            notes = "转账明细列表，包含多条收款方记录，最多支持1000条")
    private List<BatchTransferItemDTO> items;

    /** 备注 */
    @ApiModelProperty(value = "备注", example = "工资代发",
            notes = "业务备注信息")
    private String remark;

    /** 操作员 */
    @ApiModelProperty(value = "操作员", example = "admin",
            notes = "操作人标识")
    private String operator;
}
