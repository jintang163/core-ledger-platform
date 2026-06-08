package com.bank.core.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class AccountQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;

    private String accountId;

    private String accountNo;

    private Integer accountType;

    private Integer status;

    private BigDecimal minBalance;

    private BigDecimal maxBalance;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
