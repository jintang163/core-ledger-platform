package com.bank.core.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BatchTransferItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String itemId;

    private String batchId;

    private String transferId;

    private String toAccountId;

    private String toAccountNo;

    private BigDecimal amount;

    private Integer status;

    private String statusDesc;

    private String transactionId;

    private String remark;

    private String failReason;

    private LocalDateTime finishTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
