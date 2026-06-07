package com.bank.core.account.tcc;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TccTransactionContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private String xid;

    private String branchId;

    private String businessNo;

    private String fromAccountId;

    private String toAccountId;

    private String accountId;

    private BigDecimal amount;

    private String currency;

    private Long amountFen;

    private Integer freezeType;

    private String freezeLogId;

    private String transferId;

    private LocalDateTime createTime;

    private Integer phase;
}
