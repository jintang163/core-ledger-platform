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
 * 批量转账明细实体
 * 存储批量转账中的每一条转账明细记录
 * 支持部分成功场景，每条记录独立处理
 */
@Data
@TableName("t_batch_transfer_item")
public class BatchTransferItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID（雪花算法生成） */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 明细ID（业务主键） */
    private String itemId;

    /** 关联的批量转账批次ID */
    private String batchId;

    /** 关联的转账订单ID（转账成功后生成） */
    private String transferId;

    /** 收款方账户ID */
    private String toAccountId;

    /** 收款方账户号 */
    private String toAccountNo;

    /** 转账金额（元） */
    private BigDecimal amount;

    /** 状态：0-待处理 1-处理中 2-成功 3-失败 */
    private Integer status;

    /** 关联的交易ID（会计交易） */
    private String transactionId;

    /** 转账备注 */
    private String remark;

    /** 失败原因（转账失败时记录） */
    private String failReason;

    /** 完成时间（成功/失败时间） */
    private LocalDateTime finishTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除标识：0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
