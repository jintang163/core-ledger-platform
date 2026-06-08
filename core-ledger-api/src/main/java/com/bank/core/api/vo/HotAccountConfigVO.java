package com.bank.core.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class HotAccountConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String accountId;

    private String accountNo;

    private String userId;

    private Boolean isHotAccount;

    private Integer shardCount;

    private Integer shardingStrategy;

    private String shardingStrategyDesc;

    private Boolean bufferEnabled;

    private Integer bufferThreshold;

    private List<AccountShardVO> shards;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
