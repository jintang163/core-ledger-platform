package com.bank.core.api.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PaymentCallbackDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "支付订单ID不能为空")
    private String paymentId;

    @NotBlank(message = "渠道订单号不能为空")
    private String channelOrderNo;

    @NotNull(message = "渠道状态不能为空")
    private Integer channelStatus;

    private String channelRemark;

    private LocalDateTime channelTime;
}
