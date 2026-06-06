package com.bank.core.api.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 批量转账明细VO
 * 批量转账中的单条转账记录
 */
@Data
@ApiModel(value = "批量转账明细", description = "批量转账中的单条收款记录")
public class BatchTransferItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 明细ID（系统内部唯一标识） */
    @ApiModelProperty(value = "明细ID", example = "ITEM2024010100001",
            notes = "系统内部唯一标识")
    private String itemId;

    /** 关联的批量转账订单ID */
    @ApiModelProperty(value = "批量转账订单ID", example = "BAT2024010100001")
    private String batchId;

    /** 关联的转账订单ID（单条明细对应一个转账订单） */
    @ApiModelProperty(value = "转账订单ID", example = "TRF2024010100001")
    private String transferId;

    /** 收款方账户ID */
    @ApiModelProperty(value = "收款方账户ID", example = "ACC1000000002")
    private String toAccountId;

    /** 收款方账户号 */
    @ApiModelProperty(value = "收款方账户号", example = "6222029876543210987")
    private String toAccountNo;

    /** 转账金额（元） */
    @ApiModelProperty(value = "转账金额", example = "1000.00",
            notes = "单位：元")
    private BigDecimal amount;

    /** 明细状态（0-待处理, 1-处理中, 2-成功, 3-失败） */
    @ApiModelProperty(value = "明细状态", example = "2",
            notes = "0-待处理, 1-处理中, 2-成功, 3-失败")
    private Integer status;

    /** 明细状态描述 */
    @ApiModelProperty(value = "明细状态描述", example = "成功")
    private String statusDesc;

    /** 关联的会计交易ID */
    @ApiModelProperty(value = "会计交易ID", example = "TX2024010100001")
    private String transactionId;

    /** 备注 */
    @ApiModelProperty(value = "备注", example = "张三1月工资")
    private String remark;

    /** 失败原因 */
    @ApiModelProperty(value = "失败原因", example = "账户不存在")
    private String failReason;

    /** 完成时间 */
    @ApiModelProperty(value = "完成时间")
    private LocalDateTime finishTime;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}
