package com.bank.core.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AccountVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String accountId;

    private String accountNo;

    private String userId;

    private Integer accountType;

    private String accountTypeDesc;

    private String currency;

    private String currencyDesc;

    private BigDecimal balance;

    private Integer status;

    private String statusDesc;

    private Integer freezeType;

    private String freezeTypeDesc;

    private String freezeRemark;

    private LocalDateTime freezeTime;

    private String freezeOperator;

    private LocalDateTime openTime;

    private LocalDateTime closeTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
