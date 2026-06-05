package com.bank.core.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_account_freeze_log")
public class AccountFreezeLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private String logId;

    private String accountId;

    private Integer operateType;

    private Integer freezeType;

    private String remark;

    private String operator;

    private LocalDateTime operateTime;

    private LocalDateTime createTime;
}
