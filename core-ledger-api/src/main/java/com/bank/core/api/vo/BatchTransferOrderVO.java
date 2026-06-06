package com.bank.core.api.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 批量转账订单VO
 * 用于批量转账（代发）订单的返回结果
 */
@Data
@ApiModel(value = "批量转账订单", description = "批量转账（代发）订单信息")
public class BatchTransferOrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 批量转账订单ID（系统内部唯一标识） */
    @ApiModelProperty(value = "批量转账订单ID", example = "BAT2024010100001",
            notes = "系统内部唯一标识")
    private String batchId;

    /** 批量转账单号（业务可见的单号） */
    @ApiModelProperty(value = "批量转账单号", example = "BATNO2024010100001",
            notes = "业务可见的单号")
    private String batchNo;

    /** 业务流水号（调用方传入的唯一标识） */
    @ApiModelProperty(value = "业务流水号", example = "BIZ2024010100001",
            notes = "调用方传入的唯一标识")
    private String businessNo;

    /** 请求ID（用于幂等校验） */
    @ApiModelProperty(value = "请求ID", example = "REQ2024010100001",
            notes = "用于幂等校验")
    private String requestId;

    /** 付款方账户ID（所有明细的共同扣款账户） */
    @ApiModelProperty(value = "付款方账户ID", example = "ACC1000000001",
            notes = "所有明细的共同扣款账户")
    private String fromAccountId;

    /** 付款方账户号 */
    @ApiModelProperty(value = "付款方账户号", example = "6222021234567890123")
    private String fromAccountNo;

    /** 币种 */
    @ApiModelProperty(value = "币种", example = "CNY")
    private String currency;

    /** 总条数 */
    @ApiModelProperty(value = "总条数", example = "100")
    private Integer totalCount;

    /** 总金额（元） */
    @ApiModelProperty(value = "总金额", example = "100000.00",
            notes = "单位：元")
    private BigDecimal totalAmount;

    /** 成功条数 */
    @ApiModelProperty(value = "成功条数", example = "98")
    private Integer successCount;

    /** 成功金额（元） */
    @ApiModelProperty(value = "成功金额", example = "98000.00",
            notes = "单位：元")
    private BigDecimal successAmount;

    /** 失败条数 */
    @ApiModelProperty(value = "失败条数", example = "2")
    private Integer failCount;

    /** 失败金额（元） */
    @ApiModelProperty(value = "失败金额", example = "2000.00",
            notes = "单位：元")
    private BigDecimal failAmount;

    /** 订单状态（0-待处理, 1-处理中, 2-成功, 3-部分成功, 4-失败） */
    @ApiModelProperty(value = "订单状态", example = "3",
            notes = "0-待处理, 1-处理中, 2-成功, 3-部分成功, 4-失败")
    private Integer status;

    /** 订单状态描述 */
    @ApiModelProperty(value = "订单状态描述", example = "部分成功")
    private String statusDesc;

    /** 备注 */
    @ApiModelProperty(value = "备注", example = "1月工资代发")
    private String remark;

    /** 操作员 */
    @ApiModelProperty(value = "操作员", example = "admin")
    private String operator;

    /** 完成时间 */
    @ApiModelProperty(value = "完成时间")
    private LocalDateTime finishTime;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    /** 转账明细列表 */
    @ApiModelProperty(value = "转账明细列表")
    private List<BatchTransferItemVO> items;
}
