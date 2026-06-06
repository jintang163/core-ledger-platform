package com.bank.core.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_batch_transfer_order")
public class BatchTransferOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long id;

    private String batchId;

    private String batchNo;

    private String businessNo;

    private String requestId;

    private String fromAccountId;

    private String fromAccountNo;

    private String currency;

    private Integer totalCount;

    private BigDecimal totalAmount;

    private Integer successCount;

    private BigDecimal successAmount;

    private Integer failCount;

    private BigDecimal failAmount;

    private Integer status;

    private String remark;

    private String operator;

    private LocalDateTime finishTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
