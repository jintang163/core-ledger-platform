package com.bank.core.common.utils;

import cn.hutool.core.util.IdUtil;
import com.bank.core.common.service.IdGeneratorService;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

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
 * - ACC：账户ID
 * - REQ：请求ID（用于幂等校验）
 * - TRACE：链路追踪ID
 * - TXN：交易ID/交易流水号
 * - VCH：记账凭证号
 * - ENTRY：交易分录ID
 * - PAY：支付单号
 * - TRF：转账单号
 * - BAT：批量单号
 * - ITEM：批量明细ID
 * - BUF：缓冲记账流水ID
 * - FRZ：冻结日志ID
 * - MSG：消息ID
 * - CBL：回调日志ID
 * - SAGA：Saga事务ID
 * 
 * 设计说明：
 * 优先使用Spring管理的IdGeneratorService（支持配置化workerId）
 * 当Spring上下文不可用时（如静态工具类场景），降级使用默认静态实例
 */
@Component
@DependsOn("idGeneratorService")
public class SnowflakeIdGenerator {

    /** 工作机器ID（0-31） */
    private static final long DEFAULT_WORKER_ID = 1L;

    /** 数据中心ID（0-31） */
    private static final long DEFAULT_DATA_CENTER_ID = 1L;

    /** 降级使用的静态雪花算法实例 */
    private static final cn.hutool.core.lang.Snowflake DEFAULT_SNOWFLAKE =
            IdUtil.getSnowflake(DEFAULT_WORKER_ID, DEFAULT_DATA_CENTER_ID);

    /** Spring管理的ID生成服务（优先级更高） */
    private static IdGeneratorService idGeneratorService;

    @Resource
    private IdGeneratorService injectedIdGeneratorService;

    @PostConstruct
    public void init() {
        idGeneratorService = this.injectedIdGeneratorService;
    }

    /**
     * 生成雪花算法ID（long型）
     * 用于数据库主键ID
     * @return 全局唯一ID
     */
    public static long nextId() {
        if (idGeneratorService != null) {
            return idGeneratorService.nextId();
        }
        return DEFAULT_SNOWFLAKE.nextId();
    }

    /**
     * 生成雪花算法ID（字符串型）
     * @return 全局唯一ID字符串
     */
    public static String nextIdStr() {
        if (idGeneratorService != null) {
            return idGeneratorService.nextIdStr();
        }
        return DEFAULT_SNOWFLAKE.nextIdStr();
    }

    /**
     * 生成账户ID
     * 格式：ACC + 雪花ID
     * @return 账户ID
     */
    public static String generateAccountId() {
        if (idGeneratorService != null) {
            return idGeneratorService.generateAccountId();
        }
        return "ACC" + DEFAULT_SNOWFLAKE.nextIdStr();
    }

    /**
     * 生成账户号
     * 格式：622202 + 雪花ID第6-17位（共12位）
     * @return 账户号
     */
    public static String generateAccountNo() {
        if (idGeneratorService != null) {
            return idGeneratorService.generateAccountNo();
        }
        return "622202" + DEFAULT_SNOWFLAKE.nextIdStr().substring(6, 18);
    }

    /**
     * 生成请求ID
     * 格式：REQ + 时间戳 + 雪花ID后11位
     * 用于接口幂等校验
     * @return 请求ID
     */
    public static String generateRequestId() {
        if (idGeneratorService != null) {
            return idGeneratorService.generateRequestId();
        }
        return "REQ" + System.currentTimeMillis() + DEFAULT_SNOWFLAKE.nextIdStr().substring(12);
    }

    /**
     * 生成链路追踪ID
     * 格式：TRACE + 时间戳 + 雪花ID后11位
     * 用于分布式链路追踪
     * @return 追踪ID
     */
    public static String generateTraceId() {
        if (idGeneratorService != null) {
            return idGeneratorService.generateTraceId();
        }
        return "TRACE" + System.currentTimeMillis() + DEFAULT_SNOWFLAKE.nextIdStr().substring(12);
    }

    /**
     * 生成交易ID
     * 格式：TXN + 雪花ID
     * @return 交易ID
     */
    public static String generateTransactionId() {
        if (idGeneratorService != null) {
            return idGeneratorService.generateTransactionId();
        }
        return "TXN" + DEFAULT_SNOWFLAKE.nextIdStr();
    }

    /**
     * 生成交易流水号
     * 格式：TXN + 时间戳 + 雪花ID后9位
     * @return 交易流水号
     */
    public static String generateTransactionNo() {
        if (idGeneratorService != null) {
            return idGeneratorService.generateTransactionNo();
        }
        return "TXN" + System.currentTimeMillis() + DEFAULT_SNOWFLAKE.nextIdStr().substring(10);
    }

    /**
     * 生成记账凭证号
     * 格式：VCH + 时间戳 + 雪花ID后9位
     * @return 记账凭证号
     */
    public static String generateVoucherNo() {
        if (idGeneratorService != null) {
            return idGeneratorService.generateVoucherNo();
        }
        return "VCH" + System.currentTimeMillis() + DEFAULT_SNOWFLAKE.nextIdStr().substring(10);
    }

    /**
     * 生成交易分录ID
     * 格式：ENTRY + 雪花ID
     * @return 交易分录ID
     */
    public static String generateEntryId() {
        if (idGeneratorService != null) {
            return idGeneratorService.generateEntryId();
        }
        return "ENTRY" + DEFAULT_SNOWFLAKE.nextIdStr();
    }

    /**
     * 生成支付单号
     * 格式：PAY + 时间戳 + 雪花ID后9位
     * @return 支付单号
     */
    public static String generatePaymentNo() {
        if (idGeneratorService != null) {
            return idGeneratorService.generatePaymentNo();
        }
        return "PAY" + System.currentTimeMillis() + DEFAULT_SNOWFLAKE.nextIdStr().substring(10);
    }

    /**
     * 生成转账单号
     * 格式：TRF + 时间戳 + 雪花ID后9位
     * @return 转账单号
     */
    public static String generateTransferNo() {
        if (idGeneratorService != null) {
            return idGeneratorService.generateTransferNo();
        }
        return "TRF" + System.currentTimeMillis() + DEFAULT_SNOWFLAKE.nextIdStr().substring(10);
    }

    /**
     * 生成批量单号
     * 格式：BAT + 时间戳 + 雪花ID后9位
     * @return 批量单号
     */
    public static String generateBatchNo() {
        if (idGeneratorService != null) {
            return idGeneratorService.generateBatchNo();
        }
        return "BAT" + System.currentTimeMillis() + DEFAULT_SNOWFLAKE.nextIdStr().substring(10);
    }

    /**
     * 生成批量明细ID
     * 格式：ITEM + 雪花ID
     * @return 批量明细ID
     */
    public static String generateBatchItemId() {
        if (idGeneratorService != null) {
            return idGeneratorService.generateBatchItemId();
        }
        return "ITEM" + DEFAULT_SNOWFLAKE.nextIdStr();
    }

    /**
     * 生成缓冲记账流水ID
     * 格式：BUF + 时间戳 + 雪花ID后9位
     * 用于高并发缓冲记账场景
     * @return 缓冲记账流水ID
     */
    public static String generateBufferId() {
        if (idGeneratorService != null) {
            return idGeneratorService.generateBufferId();
        }
        return "BUF" + System.currentTimeMillis() + DEFAULT_SNOWFLAKE.nextIdStr().substring(10);
    }

    /**
     * 生成冻结日志ID
     * 格式：FRZ + 雪花ID
     * @return 冻结日志ID
     */
    public static String generateFreezeLogId() {
        if (idGeneratorService != null) {
            return idGeneratorService.generateFreezeLogId();
        }
        return "FRZ" + DEFAULT_SNOWFLAKE.nextIdStr();
    }

    /**
     * 生成消息ID
     * 格式：MSG + 雪花ID
     * @return 消息ID
     */
    public static String generateMessageId() {
        if (idGeneratorService != null) {
            return idGeneratorService.generateMessageId();
        }
        return "MSG" + DEFAULT_SNOWFLAKE.nextIdStr();
    }

    /**
     * 生成回调日志ID
     * 格式：CBL + 雪花ID
     * @return 回调日志ID
     */
    public static String generateCallbackLogId() {
        if (idGeneratorService != null) {
            return idGeneratorService.generateCallbackLogId();
        }
        return "CBL" + DEFAULT_SNOWFLAKE.nextIdStr();
    }

    /**
     * 生成Saga事务ID
     * 格式：SAGA + 雪花ID
     * @return Saga事务ID
     */
    public static String generateSagaId() {
        if (idGeneratorService != null) {
            return idGeneratorService.generateSagaId();
        }
        return "SAGA" + DEFAULT_SNOWFLAKE.nextIdStr();
    }

    /**
     * 生成分片ID
     * 格式：{mainAccountId}_SHARD_{index}
     * @param mainAccountId 主账户ID
     * @param shardIndex 分片索引
     * @return 分片ID
     */
    public static String generateShardId(String mainAccountId, int shardIndex) {
        if (idGeneratorService != null) {
            return idGeneratorService.generateShardId(mainAccountId, shardIndex);
        }
        return mainAccountId + "_SHARD_" + String.format("%02d", shardIndex);
    }
}
