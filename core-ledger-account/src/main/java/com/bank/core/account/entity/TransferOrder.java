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
 * 转账订单实体
 * 存储账户间转账的订单信息
 * 保证扣款方与收款方的原子操作
 */
@Data
@TableName("t_transfer_order")
public class TransferOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID（雪花算法生成） */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 转账订单ID（业务主键） */
    private String transferId;

    /** 转账单号（业务流水号，对外展示） */
    private String transferNo;

    /** 业务流水号（调用方传入，用于幂等） */
    private String businessNo;

    /** 请求ID（用于幂等校验） */
    private String requestId;

    /** 扣款方账户ID */
    private String fromAccountId;

    /** 扣款方账户号 */
    private String fromAccountNo;

    /** 收款方账户ID */
    private String toAccountId;

    /** 收款方账户号 */
    private String toAccountNo;

    /** 转账金额（元） */
    private BigDecimal amount;

    /** 币种 */
    private String currency;

    /** 订单状态：0-待处理 1-处理中 2-成功 3-失败 */
    private Integer status;

    /** 关联的交易ID（会计交易） */
    private String transactionId;

    /** 转账备注 */
    private String remark;

    /** 操作员 */
    private String operator;

    /** 转账完成时间 */
    private LocalDateTime transferTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除标识：0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
}
