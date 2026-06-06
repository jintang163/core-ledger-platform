package com.bank.core.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PaymentQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String accountId;

    private Integer paymentType;

    private Integer status;

    private String channelCode;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
