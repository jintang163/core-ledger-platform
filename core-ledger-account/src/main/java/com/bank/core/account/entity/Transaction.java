package com.bank.core.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易记录实体类
 *
 * 核心功能：
 * 1. 记录每一笔交易的基本信息
 * 2. 关联交易分录（TransactionEntry）实现复式记账
 * 3. 支持幂等性校验（通过requestId和businessNo）
 *
 * 设计要点：
 * - 每笔交易包含一个或多个借贷分录，借贷必相等
 * - 交易状态用于追踪处理进度
 * - 支持按业务流水号反向查询
 *
 * 分片策略：
 * - 分库键：account_id（通过交易分录关联）+ transaction_time 复合分片
 * - 分表键：transaction_time 按月分表（t_transaction_202506, t_transaction_202507, ...）
 * - 索引优化：(transaction_time, status), (status, transaction_time), (transaction_type, transaction_time)
 */
@Data
@TableName("t_transaction")
public class Transaction implements Serializable {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /** 主键ID（雪花算法生成） */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 交易ID（业务主键，系统唯一标识） */
    private String transactionId;

    /** 交易流水号（展示用） */
    private String transactionNo;

    /**
     * 交易类型
     * 1-充值, 2-提现, 3-转账, 4-批量转账, 5-调账等
     */
    private Integer transactionType;

    /** 业务流水号（调用方传入，用于幂等） */
    private String businessNo;

    /** 交易总金额（单位：元） */
    private BigDecimal totalAmount;

    /** 币种 */
    private String currency;

    /** 凭证号（会计凭证编号） */
    private String voucherNo;

    /** 交易摘要 */
    private String summary;

    /**
     * 交易状态
     * 0-待处理, 1-处理中, 2-成功, 3-失败, 4-已冲正
     */
    private Integer status;

    /** 请求ID（用于幂等校验） */
    private String requestId;

    /** 操作员 */
    private String operator;

    /** 交易时间 */
    private LocalDateTime transactionTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除标记（0-未删除, 1-已删除） */
    private Integer deleted;
}
