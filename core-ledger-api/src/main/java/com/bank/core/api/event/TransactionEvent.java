package com.bank.core.api.event;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;

    private String eventType;

    private String transactionId;

    private String businessNo;

    private String fromAccountId;

    private String fromAccountNo;

    private String toAccountId;

    private String toAccountNo;

    private BigDecimal amount;

    private String currency;

    private Integer transactionType;

    private String status;

    private String channelCode;

    private String remark;

    private String operator;

    private String requestId;

    private LocalDateTime eventTime;
}
