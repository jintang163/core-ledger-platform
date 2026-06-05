package com.bank.core.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_transaction_entry")
public class TransactionEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private String entryId;

    private String transactionId;

    private String accountId;

    private String accountNo;

    private String subjectCode;

    private String subjectName;

    private Integer direction;

    private BigDecimal amount;

    private String currency;

    private String summary;

    private LocalDateTime createTime;
}
