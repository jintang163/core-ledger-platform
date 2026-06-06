package com.bank.core.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 交易信息展示对象（VO）
 * 
 * 用于向调用方返回交易的完整信息，包括交易基本信息和分录明细。
 * 
 * 字段说明：
 * - 以 Desc 结尾的字段为枚举值的中文描述，用于前端直接展示
 * - entries 字段包含完整的借贷分录信息
 * - remark 字段用于特殊场景的说明（如缓冲记账提示）
 */
@Data
public class TransactionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 交易唯一标识ID */
    private String transactionId;

    /** 交易流水号（对外展示的业务流水号） */
    private String transactionNo;

    /** 交易类型代码（见 TransactionTypeEnum） */
    private Integer transactionType;

    /** 交易类型中文描述 */
    private String transactionTypeDesc;

    /** 业务单号（调用方传入的业务唯一标识） */
    private String businessNo;

    /** 交易总金额（元） */
    private BigDecimal totalAmount;

    /** 币种（见 CurrencyEnum） */
    private String currency;

    /** 记账凭证号 */
    private String voucherNo;

    /** 交易摘要 */
    private String summary;

    /** 交易状态代码（见 TransactionStatusEnum） */
    private Integer status;

    /** 交易状态中文描述 */
    private String statusDesc;

    /** 请求ID（用于幂等校验和链路追踪） */
    private String requestId;

    /** 操作人 */
    private String operator;

    /** 交易发生时间 */
    private LocalDateTime transactionTime;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 交易分录明细列表（借贷双方） */
    private List<TransactionEntryVO> entries;

    /** 备注信息（用于特殊场景说明，如缓冲记账处理中提示） */
    private String remark;
}
