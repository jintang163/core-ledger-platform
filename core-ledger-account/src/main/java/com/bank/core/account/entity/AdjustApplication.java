package com.bank.core.account.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_adjust_application")
public class AdjustApplication {

    private Long id;

    private String applicationId;

    private String applicationNo;

    private String accountId;

    private String accountNo;

    private String userId;

    private Integer adjustType;

    private BigDecimal amount;

    private String currency;

    private String reason;

    private Integer status;

    private String applicant;

    private LocalDateTime applyTime;

    private String approver;

    private LocalDateTime approveTime;

    private String approveRemark;

    private String executor;

    private LocalDateTime executeTime;

    private String transactionId;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
