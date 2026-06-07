package com.bank.core.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易分录实体类
 *
 * 核心功能：
 * 1. 实现复式记账的借方和贷方分录
 * 2. 每笔交易至少包含一借一贷两条分录
 * 3. 关联会计科目，支持财务核算
 *
 * 设计要点：
 * - 每条分录对应一个账户的资金变动
 * - 借贷方向：1-借方, 2-贷方
 * - 所有分录的借方金额合计 = 贷方金额合计
 * - 支持按科目代码进行财务统计
 *
 * 分片策略：
 * - 分库键：account_id + create_time 复合分片（与交易表保持一致）
 * - 分表键：create_time 按月分表（t_transaction_entry_202506, ...）
 * - 索引优化：(account_id, create_time), (account_id, transaction_id), (create_time)
 */
@Data
@TableName("t_transaction_entry")
public class TransactionEntry implements Serializable {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /** 主键ID（雪花算法生成） */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 分录ID（业务主键） */
    private String entryId;

    /** 关联的交易ID */
    private String transactionId;

    /** 账户ID */
    private String accountId;

    /** 账户号 */
    private String accountNo;

    /** 会计科目代码 */
    private String subjectCode;

    /** 会计科目名称 */
    private String subjectName;

    /**
     * 借贷方向
     * 1-借方（资产/成本/费用增加，负债/权益/收入减少）
     * 2-贷方（负债/权益/收入增加，资产/成本/费用减少）
     */
    private Integer direction;

    /** 分录金额（单位：元） */
    private BigDecimal amount;

    /** 币种 */
    private String currency;

    /** 分录摘要 */
    private String summary;

    /** 创建时间 */
    private LocalDateTime createTime;
}
