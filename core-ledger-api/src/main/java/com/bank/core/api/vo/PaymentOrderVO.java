package com.bank.core.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentOrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String paymentId;

    private String paymentNo;

    private String businessNo;

    private String requestId;

    private Integer paymentType;

    private String paymentTypeDesc;

    private String accountId;

    private String accountNo;

    private BigDecimal amount;

    private String currency;

    private String channelCode;

    private String channelOrderNo;

    private Integer status;

    private String statusDesc;

    private Integer channelStatus;

    private String channelStatusDesc;

    private String remark;

    private String operator;

    private LocalDateTime channelTime;

    private LocalDateTime successTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
