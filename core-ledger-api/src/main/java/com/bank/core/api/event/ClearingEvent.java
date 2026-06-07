package com.bank.core.api.event;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ClearingEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;

    private String eventType;

    private String clearingId;

    private String transactionId;

    private String accountId;

    private String accountNo;

    private BigDecimal amount;

    private String currency;

    private Integer clearingType;

    private String status;

    private String businessNo;

    private String remark;

    private LocalDateTime eventTime;
}
