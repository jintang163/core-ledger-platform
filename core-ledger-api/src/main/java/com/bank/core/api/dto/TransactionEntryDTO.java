package com.bank.core.api.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 交易分录请求DTO
 *
 * 核心功能：
 * 1. 封装单条交易分录的详细信息
 * 2. 作为TransactionCreateDTO的嵌套对象
 * 3. 定义每条分录的账户、科目、方向、金额等信息
 *
 * 设计要点：
 * - 每条分录对应一个账户的资金变动
 * - 金额必须大于0，由direction字段表示增减方向
 * - 会计科目用于财务核算和报表生成
 * - 同一笔交易的所有分录金额合计必须借贷平衡
 */
@Data
public class TransactionEntryDTO implements Serializable {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /**
     * 账户ID（必填）
     * 本条分录对应的账户
     */
    @NotBlank(message = "账户ID不能为空")
    private String accountId;

    /**
     * 会计科目代码（必填）
     * 如：1001-库存现金, 2001-短期借款等
     * 用于财务核算和分类统计
     */
    @NotBlank(message = "科目代码不能为空")
    private String subjectCode;

    /**
     * 会计科目名称（必填）
     * 与科目代码对应，便于展示
     */
    @NotBlank(message = "科目名称不能为空")
    private String subjectName;

    /**
     * 借贷方向（必填）
     * 1-借方：资产/成本/费用增加，负债/权益/收入减少
     * 2-贷方：负债/权益/收入增加，资产/成本/费用减少
     */
    @NotNull(message = "借贷方向不能为空")
    private Integer direction;

    /**
     * 分录金额（必填，单位：元）
     * 必须大于0，借贷方向由direction字段控制
     * 使用@Positive确保金额为正数
     */
    @NotNull(message = "金额不能为空")
    @Positive(message = "金额必须大于0")
    private BigDecimal amount;

    /**
     * 分录摘要（选填）
     * 本条分录的具体说明
     */
    private String summary;
}
