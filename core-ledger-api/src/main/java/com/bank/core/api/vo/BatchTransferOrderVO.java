package com.bank.core.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BatchTransferOrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

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

    private String statusDesc;

    private String remark;

    private String operator;

    private LocalDateTime finishTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<BatchTransferItemVO> items;
}
