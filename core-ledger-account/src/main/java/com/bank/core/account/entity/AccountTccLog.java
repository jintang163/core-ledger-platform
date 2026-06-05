package com.bank.core.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_account_tcc_log")
public class AccountTccLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private String txId;

    private String actionName;

    private Integer phase;

    private String accountId;

    private String context;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
