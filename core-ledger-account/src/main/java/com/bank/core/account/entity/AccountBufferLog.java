package com.bank.core.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户缓冲记账流水实体
 * 用于缓冲记账场景：先记录流水，后异步批量更新余额
 * 适用于允许短暂数据不一致的高并发场景（如批量代发、批量扣款等）
 *
 * 业务流程：
 * 1. 业务请求时，先写入缓冲流水（状态：待处理）
 * 2. 立即返回成功给调用方
 * 3. 后台定时任务批量处理流水，更新账户余额
 * 4. 处理完成后更新流水状态
 */
@Data
@TableName("t_account_buffer_log")
@ApiModel(value = "缓冲记账流水", description = "高并发场景下的缓冲记账流水")
public class AccountBufferLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    @ApiModelProperty(value = "主键ID", hidden = true)
    private Long id;

    /** 缓冲流水ID */
    @ApiModelProperty(value = "缓冲流水ID", example = "BUF2024010100001",
            notes = "系统唯一标识")
    private String bufferId;

    /** 业务流水号（调用方传入） */
    @ApiModelProperty(value = "业务流水号", example = "BIZ2024010100001",
            notes = "调用方传入的唯一标识")
    private String businessNo;

    /** 请求ID（用于幂等） */
    @ApiModelProperty(value = "请求ID", example = "REQ2024010100001",
            notes = "用于幂等校验")
    private String requestId;

    /** 账户ID */
    @ApiModelProperty(value = "账户ID", example = "ACC1000001")
    private String accountId;

    /** 账户号 */
    @ApiModelProperty(value = "账户号", example = "6222021234567890123")
    private String accountNo;

    /** 变动金额（元，正数增加，负数减少） */
    @ApiModelProperty(value = "变动金额", example = "100.00",
            notes = "单位：元，正数表示增加，负数表示减少")
    private BigDecimal amount;

    /** 变动金额（分） */
    @ApiModelProperty(value = "变动金额（分）", example = "10000",
            notes = "单位：分，正数表示增加，负数表示减少")
    private Long amountFen;

    /** 币种 */
    @ApiModelProperty(value = "币种", example = "CNY")
    private String currency;

    /** 交易类型（1-充值, 2-提现, 3-转账, 4-批量转账） */
    @ApiModelProperty(value = "交易类型", example = "3",
            notes = "1-充值, 2-提现, 3-转账, 4-批量转账")
    private Integer transactionType;

    /** 状态（0-待处理, 1-处理中, 2-处理成功, 3-处理失败） */
    @ApiModelProperty(value = "状态", example = "0",
            notes = "0-待处理, 1-处理中, 2-处理成功, 3-处理失败")
    private Integer status;

    /** 处理批次号 */
    @ApiModelProperty(value = "处理批次号", example = "BATCH2024010100001",
            notes = "批量处理时的批次号")
    private String batchNo;

    /** 重试次数 */
    @ApiModelProperty(value = "重试次数", example = "0",
            notes = "处理失败后的重试次数")
    private Integer retryCount;

    /** 错误信息 */
    @ApiModelProperty(value = "错误信息", example = "余额不足",
            notes = "处理失败时的错误原因")
    private String errorMsg;

    /** 处理完成时间 */
    @ApiModelProperty(value = "处理完成时间")
    private LocalDateTime processTime;

    /** 关联的交易ID */
    @ApiModelProperty(value = "关联交易ID", example = "TX2024010100001",
            notes = "处理完成后关联的正式交易ID")
    private String transactionId;

    /** 备注 */
    @ApiModelProperty(value = "备注", example = "批量代发")
    private String remark;

    /** 操作员 */
    @ApiModelProperty(value = "操作员", example = "admin")
    private String operator;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @ApiModelProperty(hidden = true)
    private Integer deleted;
}
