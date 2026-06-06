package com.bank.core.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransferOrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

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

    private String statusDesc;

    private String transactionId;

    private String remark;

    private String operator;

    private LocalDateTime transferTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
