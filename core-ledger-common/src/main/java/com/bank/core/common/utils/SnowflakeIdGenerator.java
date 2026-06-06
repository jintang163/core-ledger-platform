package com.bank.core.common.utils;

import cn.hutool.core.util.IdUtil;

/**
 * 雪花算法ID生成器工具类
 * 
 * 基于Twitter雪花算法（Snowflake）生成全局唯一ID
 * 算法结构：1位符号位 + 41位时间戳 + 10位工作节点（5位数据中心 + 5位工作机器） + 12位序列号
 * 
 * 核心特性：
 * 1. 全局唯一性：保证分布式环境下不重复
 * 2. 时间有序性：ID按时间递增，便于数据库索引优化
 * 3. 高性能：每秒可生成约400万个ID
 * 
 * ID前缀约定：
 * - 无前缀：主键ID（long型）
 * - REQ：请求ID（用于幂等校验）
 * - TRACE：链路追踪ID
 * - TXN：交易流水号
 * - VCH：记账凭证号
 * - ENTRY：交易分录ID
 * - PAY：支付单号
 * - TRF：转账单号
 * - BAT：批量单号
 * - ITEM：批量明细ID
 * - BUF：缓冲记账流水ID
 */
public class SnowflakeIdGenerator {

    /** 工作机器ID（0-31） */
    private static final long WORKER_ID = 1L;

    /** 数据中心ID（0-31） */
    private static final long DATA_CENTER_ID = 1L;

    /** 雪花算法实例 */
    private static final cn.hutool.core.lang.Snowflake SNOWFLAKE =
            IdUtil.getSnowflake(WORKER_ID, DATA_CENTER_ID);

    /**
     * 生成雪花算法ID（long型）
     * 用于数据库主键ID
     * @return 全局唯一ID
     */
    public static long nextId() {
        return SNOWFLAKE.nextId();
    }

    /**
     * 生成雪花算法ID（字符串型）
     * @return 全局唯一ID字符串
     */
    public static String nextIdStr() {
        return SNOWFLAKE.nextIdStr();
    }

    /**
     * 生成账户号
     * 格式：622202 + 雪花ID第6-17位（共12位）
     * @return 账户号
     */
    public static String generateAccountNo() {
        return "622202" + SNOWFLAKE.nextIdStr().substring(6, 18);
    }

    /**
     * 生成请求ID
     * 格式：REQ + 时间戳 + 雪花ID后11位
     * 用于接口幂等校验
     * @return 请求ID
     */
    public static String generateRequestId() {
        return "REQ" + System.currentTimeMillis() + SNOWFLAKE.nextIdStr().substring(12);
    }

    /**
     * 生成链路追踪ID
     * 格式：TRACE + 时间戳 + 雪花ID后11位
     * 用于分布式链路追踪
     * @return 追踪ID
     */
    public static String generateTraceId() {
        return "TRACE" + System.currentTimeMillis() + SNOWFLAKE.nextIdStr().substring(12);
    }

    /**
     * 生成交易流水号
     * 格式：TXN + 时间戳 + 雪花ID后9位
     * @return 交易流水号
     */
    public static String generateTransactionNo() {
        return "TXN" + System.currentTimeMillis() + SNOWFLAKE.nextIdStr().substring(10);
    }

    /**
     * 生成记账凭证号
     * 格式：VCH + 时间戳 + 雪花ID后9位
     * @return 记账凭证号
     */
    public static String generateVoucherNo() {
        return "VCH" + System.currentTimeMillis() + SNOWFLAKE.nextIdStr().substring(10);
    }

    /**
     * 生成交易分录ID
     * 格式：ENTRY + 雪花ID
     * @return 交易分录ID
     */
    public static String generateEntryId() {
        return "ENTRY" + SNOWFLAKE.nextIdStr();
    }

    /**
     * 生成支付单号
     * 格式：PAY + 时间戳 + 雪花ID后9位
     * @return 支付单号
     */
    public static String generatePaymentNo() {
        return "PAY" + System.currentTimeMillis() + SNOWFLAKE.nextIdStr().substring(10);
    }

    /**
     * 生成转账单号
     * 格式：TRF + 时间戳 + 雪花ID后9位
     * @return 转账单号
     */
    public static String generateTransferNo() {
        return "TRF" + System.currentTimeMillis() + SNOWFLAKE.nextIdStr().substring(10);
    }

    /**
     * 生成批量单号
     * 格式：BAT + 时间戳 + 雪花ID后9位
     * @return 批量单号
     */
    public static String generateBatchNo() {
        return "BAT" + System.currentTimeMillis() + SNOWFLAKE.nextIdStr().substring(10);
    }

    /**
     * 生成批量明细ID
     * 格式：ITEM + 雪花ID
     * @return 批量明细ID
     */
    public static String generateBatchItemId() {
        return "ITEM" + SNOWFLAKE.nextIdStr();
    }

    /**
     * 生成缓冲记账流水ID
     * 格式：BUF + 时间戳 + 雪花ID后9位
     * 用于高并发缓冲记账场景
     * @return 缓冲记账流水ID
     */
    public static String generateBufferId() {
        return "BUF" + System.currentTimeMillis() + SNOWFLAKE.nextIdStr().substring(10);
    }
}
