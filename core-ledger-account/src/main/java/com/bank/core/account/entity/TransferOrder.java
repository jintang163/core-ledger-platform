package com.bank.core.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_transfer_order")
public class TransferOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private String transferId;

    private String transferNo;

    private String businessNo;

    private String requestId;

    private String fromAccountId;

    private String fromAccountNo;

    private String toAccountId;

    private String toAccountNo;

    private BigDecimal amount;

    private String currency;

    private Integer status;

    private String transactionId;

    private String remark;

    private String operator;

    private LocalDateTime transferTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
