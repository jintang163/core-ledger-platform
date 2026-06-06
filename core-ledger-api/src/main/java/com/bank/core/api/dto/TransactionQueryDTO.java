package com.bank.core.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 交易查询请求DTO
 *
 * 核心功能：
 * 1. 接收交易列表查询的筛选条件
 * 2. 支持多维度组合查询（账户、类型、状态、时间范围）
 * 3. 封装分页参数
 *
 * 设计要点：
 * - 所有查询条件均为可选，支持灵活组合
 * - 分页参数设置默认值，简化调用方使用
 * - 时间范围默认查询最近一个月的数据
 * - 查询结果按创建时间倒序排列
 */
@Data
public class TransactionQueryDTO implements Serializable {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /**
     * 账户ID（选填）
     * 查询该账户相关的所有交易（作为借方或贷方）
     */
    private String accountId;

    /**
     * 交易类型（选填）
     * 1-充值, 2-提现, 3-转账, 4-批量转账等
     */
    private Integer transactionType;

    /**
     * 交易状态（选填）
     * 0-待处理, 1-处理中, 2-成功, 3-失败, 4-已冲正
     */
    private Integer status;

    /**
     * 查询起始时间（选填）
     * 交易创建时间 >= startTime
     */
    private LocalDateTime startTime;

    /**
     * 查询结束时间（选填）
     * 交易创建时间 < endTime
     */
    private LocalDateTime endTime;

    /**
     * 页码（选填，默认：1）
     * 从1开始计数
     */
    private Integer pageNum = 1;

    /**
     * 每页条数（选填，默认：20）
     * 建议最大值不超过100
     */
    private Integer pageSize = 20;
}
