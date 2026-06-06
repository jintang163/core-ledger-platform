package com.bank.core.api.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 交易创建请求DTO
 *
 * 核心功能：
 * 1. 接收前端/调用方的记账请求参数
 * 2. 封装交易基本信息和分录明细
 * 3. 通过JSR-380注解进行参数校验
 *
 * 设计要点：
 * - 请求ID和业务流水号用于幂等性校验
 * - 分录列表必须至少包含一条借方和一条贷方分录
 * - 使用@Valid注解启用嵌套校验（校验分录列表中的每个元素）
 * - 所有金额单位为元，由服务层转换为分进行存储
 */
@Data
public class TransactionCreateDTO implements Serializable {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /**
     * 请求ID（必填）
     * 用于幂等性校验，同一请求ID只能处理一次
     * 建议使用UUID或雪花算法生成
     */
    @NotBlank(message = "请求ID不能为空")
    private String requestId;

    /**
     * 业务流水号（必填）
     * 调用方业务系统的唯一标识
     * 用于反向查询和对账
     */
    @NotBlank(message = "业务流水号不能为空")
    private String businessNo;

    /**
     * 交易类型（必填）
     * 1-充值, 2-提现, 3-转账, 4-批量转账, 5-调账等
     */
    @NotNull(message = "交易类型不能为空")
    private Integer transactionType;

    /**
     * 币种（必填）
     * CNY-人民币, USD-美元等
     */
    @NotBlank(message = "币种不能为空")
    private String currency;

    /**
     * 交易总金额（必填，单位：元）
     * 必须等于所有分录金额的合计
     */
    @NotNull(message = "交易金额不能为空")
    private BigDecimal totalAmount;

    /**
     * 交易摘要（选填）
     * 简要描述交易用途
     */
    private String summary;

    /**
     * 操作员（选填）
     * 记录操作人信息，用于审计
     */
    private String operator;

    /**
     * 交易分录列表（必填）
     * 必须至少包含一条借方和一条贷方分录
     * 所有分录的借方金额合计 = 贷方金额合计 = 交易总金额
     * 使用@Valid注解启用嵌套校验
     */
    @NotEmpty(message = "分录不能为空")
    @Valid
    private List<TransactionEntryDTO> entries;
}
