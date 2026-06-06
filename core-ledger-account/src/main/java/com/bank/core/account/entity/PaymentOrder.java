package com.bank.core.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_payment_order")
public class PaymentOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private String paymentId;

    private String paymentNo;

    private String businessNo;

    private String requestId;

    private Integer paymentType;

    private String accountId;

    private String accountNo;

    private BigDecimal amount;

    private String currency;

    private String channelCode;

    private String channelOrderNo;

    private Integer status;

    private Integer channelStatus;

    private String callbackUrl;

    private String remark;

    private String operator;

    private LocalDateTime channelTime;

    private LocalDateTime successTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
