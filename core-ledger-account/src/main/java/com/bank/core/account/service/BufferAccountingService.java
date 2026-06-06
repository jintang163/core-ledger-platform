package com.bank.core.account.service;

import com.bank.core.account.entity.AccountBufferLog;

import java.math.BigDecimal;
import java.util.List;

/**
 * 缓冲记账服务接口
 * 提供流水记录、异步批量更新余额等功能
 * 适用于允许短暂数据不一致的高并发场景
 */
public interface BufferAccountingService {

    /**
     * 记录缓冲流水（先记账，后更新余额）
     * @param requestId 请求ID（用于幂等）
     * @param businessNo 业务流水号
     * @param accountId 账户ID
     * @param amount 变动金额（元），正数增加，负数减少
     * @param currency 币种
     * @param transactionType 交易类型
     * @param remark 备注
     * @param operator 操作员
     * @return 缓冲流水ID
     */
    String recordBufferLog(String requestId, String businessNo, String accountId,
                           BigDecimal amount, String currency, Integer transactionType,
                           String remark, String operator);

    /**
     * 批量处理缓冲流水，更新账户余额
     * @param batchSize 批量大小
     * @return 处理成功的数量
     */
    int processBufferLogs(int batchSize);

    /**
     * 定时处理缓冲流水（定时任务调用）
     */
    void scheduledProcessBufferLogs();

    /**
     * 查询账户的待处理缓冲流水总金额
     * 用于查询账户真实可用余额时使用
     * @param accountId 账户ID
     * @return 待处理金额合计（分），正数表示待入账，负数表示待扣款
     */
    Long getPendingAmount(String accountId);

    /**
     * 查询账户的可用余额（账户余额 + 待处理缓冲金额）
     * @param accountId 账户ID
     * @return 可用余额（分）
     */
    Long getAvailableBalance(String accountId);

    /**
     * 根据缓冲流水ID查询
     * @param bufferId 缓冲流水ID
     * @return 缓冲流水实体
     */
    AccountBufferLog getBufferLog(String bufferId);

    /**
     * 根据业务流水号查询
     * @param businessNo 业务流水号
     * @return 缓冲流水实体
     */
    AccountBufferLog getBufferLogByBusinessNo(String businessNo);

    /**
     * 重试处理失败的缓冲流水
     * @param bufferId 缓冲流水ID
     * @return 是否处理成功
     */
    boolean retryProcessBufferLog(String bufferId);

    /**
     * 查询待处理的缓冲流水列表
     * @param limit 最大数量
     * @return 缓冲流水列表
     */
    List<AccountBufferLog> getPendingLogs(int limit);

    /**
     * 判断账户是否启用缓冲记账
     * @param accountId 账户ID
     * @return 是否启用缓冲记账
     */
    boolean isBufferEnabled(String accountId);
}
