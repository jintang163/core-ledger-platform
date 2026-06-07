package com.bank.core.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_reliable_message")
public class ReliableMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private String messageId;

    private String messageType;

    private String topic;

    private String tag;

    private String messageKey;

    private String messageContent;

    private String businessNo;

    private String status;

    private Integer retryCount;

    private Integer maxRetryTimes;

    private Long nextRetryTime;

    private String errorMsg;

    private LocalDateTime sendTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
