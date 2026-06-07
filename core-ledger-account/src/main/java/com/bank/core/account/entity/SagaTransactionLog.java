package com.bank.core.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_saga_transaction_log")
public class SagaTransactionLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private String sagaId;

    private String sagaName;

    private String businessNo;

    private Integer transactionType;

    private Integer status;

    private String stepId;

    private String stepName;

    private Integer phase;

    private Integer stepStatus;

    private Integer retryCount;

    private String params;

    private String errorMessage;

    private LocalDateTime executeTime;

    private LocalDateTime completeTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
