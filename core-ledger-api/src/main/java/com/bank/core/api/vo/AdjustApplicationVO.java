package com.bank.core.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdjustApplicationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String applicationNo;

    private String accountId;

    private String accountNo;

    private String userId;

    private Integer adjustType;

    private String adjustTypeDesc;

    private BigDecimal amount;

    private String currency;

    private String reason;

    private Integer status;

    private String statusDesc;

    private String applicant;

    private LocalDateTime applyTime;

    private String approver;

    private LocalDateTime approveTime;

    private String approveRemark;

    private String executor;

    private LocalDateTime executeTime;

    private String transactionId;

    private String remark;
}
