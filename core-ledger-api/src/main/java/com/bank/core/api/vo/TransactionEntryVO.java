package com.bank.core.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionEntryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String entryId;

    private String transactionId;

    private String accountId;

    private String accountNo;

    private String subjectCode;

    private String subjectName;

    private Integer direction;

    private String directionDesc;

    private BigDecimal amount;

    private String currency;

    private String summary;

    private LocalDateTime createTime;
}
