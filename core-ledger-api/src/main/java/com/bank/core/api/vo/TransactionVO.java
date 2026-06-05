package com.bank.core.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TransactionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String transactionId;

    private String transactionNo;

    private Integer transactionType;

    private String transactionTypeDesc;

    private String businessNo;

    private BigDecimal totalAmount;

    private String currency;

    private String voucherNo;

    private String summary;

    private Integer status;

    private String statusDesc;

    private String operator;

    private LocalDateTime transactionTime;

    private LocalDateTime createTime;

    private List<TransactionEntryVO> entries;
}
