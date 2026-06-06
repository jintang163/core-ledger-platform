package com.bank.core.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 批量转账主单实体
 * 存储批量转账的汇总信息
 * 支持部分成功，记录成功和失败的统计信息
 */
@Data
@TableName("t_batch_transfer_order")
public class BatchTransferOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID（雪花算法生成） */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 批量转账批次ID（业务主键） */
    private String batchId;

    /** 批量转账批次号（业务流水号，对外展示） */
    private String batchNo;

    /** 业务流水号（调用方传入，用于幂等） */
    private String businessNo;

    /** 请求ID（用于幂等校验） */
    private String requestId;

    /** 扣款方账户ID（所有明细的共同扣款账户） */
    private String fromAccountId;

    /** 扣款方账户号 */
    private String fromAccountNo;

    /** 币种 */
    private String currency;

    /** 转账总笔数 */
    private Integer totalCount;

    /** 转账总金额（元） */
    private BigDecimal totalAmount;

    /** 成功笔数 */
    private Integer successCount;

    /** 成功金额（元） */
    private BigDecimal successAmount;

    /** 失败笔数 */
    private Integer failCount;

    /** 失败金额（元） */
    private BigDecimal failAmount;

    /** 批次状态：0-待处理 1-处理中 2-全部成功 3-全部失败 4-部分成功 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 操作员 */
    private String operator;

    /** 批次完成时间 */
    private LocalDateTime finishTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除标识：0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
