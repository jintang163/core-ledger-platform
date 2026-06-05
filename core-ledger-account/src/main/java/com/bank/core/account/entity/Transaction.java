package com.bank.core.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_transaction")
public class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private String transactionId;

    private String transactionNo;

    private Integer transactionType;

    private String businessNo;

    private BigDecimal totalAmount;

    private String currency;

    private String voucherNo;

    private String summary;

    private Integer status;

    private String requestId;

    private String operator;

    private LocalDateTime transactionTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
