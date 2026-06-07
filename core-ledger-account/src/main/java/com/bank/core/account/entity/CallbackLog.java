package com.bank.core.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_callback_log")
public class CallbackLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private String logId;

    private String businessNo;

    private String callbackType;

    private String callbackUrl;

    private String requestMethod;

    private String requestBody;

    private String requestHeaders;

    private String responseBody;

    private Integer responseStatus;

    private String status;

    private Integer retryCount;

    private Integer maxRetryTimes;

    private Long nextRetryTime;

    private String errorMsg;

    private LocalDateTime executeTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
