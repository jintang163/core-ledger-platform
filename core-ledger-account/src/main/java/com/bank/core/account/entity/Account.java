package com.bank.core.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_account")
public class Account implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private String accountId;

    private String accountNo;

    private String userId;

    private Integer accountType;

    private String currency;

    private Long balance;

    private Integer status;

    private Integer freezeType;

    private String freezeRemark;

    private LocalDateTime freezeTime;

    private String freezeOperator;

    private LocalDateTime openTime;

    private LocalDateTime closeTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
