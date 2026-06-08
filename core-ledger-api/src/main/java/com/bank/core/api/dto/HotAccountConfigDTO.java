package com.bank.core.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class HotAccountConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String accountId;

    private Integer shardCount;

    private Integer shardingStrategy;

    private Boolean bufferEnabled;

    private Integer bufferThreshold;

    private String requestId;
}
