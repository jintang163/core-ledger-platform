package com.bank.core.api.event;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AccountEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;

    private String eventType;

    private String accountId;

    private String accountNo;

    private String userId;

    private Integer accountType;

    private String currency;

    private BigDecimal balance;

    private Integer oldStatus;

    private Integer newStatus;

    private Integer freezeType;

    private String remark;

    private String operator;

    private String requestId;

    private String transactionId;

    private Integer transactionType;

    private LocalDateTime eventTime;
}
