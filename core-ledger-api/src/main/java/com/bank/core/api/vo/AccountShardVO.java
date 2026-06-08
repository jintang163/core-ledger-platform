package com.bank.core.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AccountShardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String shardId;

    private String mainAccountId;

    private Integer shardIndex;

    private BigDecimal balance;

    private Integer status;

    private String statusDesc;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
