package com.bank.core.common.service;

import cn.hutool.core.util.IdUtil;
import com.bank.core.common.config.SnowflakeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 分布式全局唯一ID生成服务
 * 
 * 基于Twitter雪花算法（Snowflake）实现，支持Spring配置注入。
 * 
 * 算法结构：
 * 1位符号位 + 41位时间戳 + 10位工作节点（5位数据中心 + 5位工作机器） + 12位序列号
 * 
 * 核心特性：
 * 1. 全局唯一性：保证分布式环境下不重复
 * 2. 时间有序性：ID按时间递增，便于数据库索引优化
 * 3. 高性能：单节点每秒可生成约400万个ID
 * 4. 可配置性：支持通过配置文件调整workerId和dataCenterId
 * 
 * 使用场景：
 * - 数据库主键ID
 * - 业务流水号（订单号、凭证号、交易号等）
 * - 分布式链路追踪ID
 * - 消息队列消息ID
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdGeneratorService {

    private final SnowflakeProperties properties;

    private cn.hutool.core.lang.Snowflake snowflake;

    @PostConstruct
    public void init() {
        long workerId = properties.getWorkerId();
        long dataCenterId = properties.getDataCenterId();
        
        log.info("初始化雪花算法ID生成器, workerId: {}, dataCenterId: {}", workerId, dataCenterId);
        
        if (workerId < 0 || workerId > 31) {
            throw new IllegalArgumentException("workerId must be between 0 and 31");
        }
        if (dataCenterId < 0 || dataCenterId > 31) {
            throw new IllegalArgumentException("dataCenterId must be between 0 and 31");
        }
        
        this.snowflake = IdUtil.getSnowflake(workerId, dataCenterId);
    }

    /**
     * 生成雪花算法ID（long型）
     * 用于数据库主键ID
     * @return 全局唯一ID
     */
    public long nextId() {
        return snowflake.nextId();
    }

    /**
     * 生成雪花算法ID（字符串型）
     * @return 全局唯一ID字符串
     */
    public String nextIdStr() {
        return snowflake.nextIdStr();
    }

    /**
     * 生成账户ID
     * 格式：ACC + 雪花ID
     * @return 账户ID
     */
    public String generateAccountId() {
        return "ACC" + snowflake.nextIdStr();
    }

    /**
     * 生成账户号
     * 格式：622202 + 雪花ID第6-17位（共12位）
     * @return 账户号
     */
    public String generateAccountNo() {
        return "622202" + snowflake.nextIdStr().substring(6, 18);
    }

    /**
     * 生成请求ID
     * 格式：REQ + 时间戳 + 雪花ID后11位
     * 用于接口幂等校验
     * @return 请求ID
     */
    public String generateRequestId() {
        return "REQ" + System.currentTimeMillis() + snowflake.nextIdStr().substring(12);
    }

    /**
     * 生成链路追踪ID
     * 格式：TRACE + 时间戳 + 雪花ID后11位
     * 用于分布式链路追踪
     * @return 追踪ID
     */
    public String generateTraceId() {
        return "TRACE" + System.currentTimeMillis() + snowflake.nextIdStr().substring(12);
    }

    /**
     * 生成交易ID
     * 格式：TXN + 雪花ID
     * @return 交易ID
     */
    public String generateTransactionId() {
        return "TXN" + snowflake.nextIdStr();
    }

    /**
     * 生成交易流水号
     * 格式：TXN + 时间戳 + 雪花ID后9位
     * @return 交易流水号
     */
    public String generateTransactionNo() {
        return "TXN" + System.currentTimeMillis() + snowflake.nextIdStr().substring(10);
    }

    /**
     * 生成记账凭证号
     * 格式：VCH + 时间戳 + 雪花ID后9位
     * @return 记账凭证号
     */
    public String generateVoucherNo() {
        return "VCH" + System.currentTimeMillis() + snowflake.nextIdStr().substring(10);
    }

    /**
     * 生成交易分录ID
     * 格式：ENTRY + 雪花ID
     * @return 交易分录ID
     */
    public String generateEntryId() {
        return "ENTRY" + snowflake.nextIdStr();
    }

    /**
     * 生成支付单号
     * 格式：PAY + 时间戳 + 雪花ID后9位
     * @return 支付单号
     */
    public String generatePaymentNo() {
        return "PAY" + System.currentTimeMillis() + snowflake.nextIdStr().substring(10);
    }

    /**
     * 生成转账单号
     * 格式：TRF + 时间戳 + 雪花ID后9位
     * @return 转账单号
     */
    public String generateTransferNo() {
        return "TRF" + System.currentTimeMillis() + snowflake.nextIdStr().substring(10);
    }

    /**
     * 生成批量单号
     * 格式：BAT + 时间戳 + 雪花ID后9位
     * @return 批量单号
     */
    public String generateBatchNo() {
        return "BAT" + System.currentTimeMillis() + snowflake.nextIdStr().substring(10);
    }

    /**
     * 生成批量明细ID
     * 格式：ITEM + 雪花ID
     * @return 批量明细ID
     */
    public String generateBatchItemId() {
        return "ITEM" + snowflake.nextIdStr();
    }

    /**
     * 生成缓冲记账流水ID
     * 格式：BUF + 时间戳 + 雪花ID后9位
     * 用于高并发缓冲记账场景
     * @return 缓冲记账流水ID
     */
    public String generateBufferId() {
        return "BUF" + System.currentTimeMillis() + snowflake.nextIdStr().substring(10);
    }

    /**
     * 生成冻结日志ID
     * 格式：FRZ + 雪花ID
     * @return 冻结日志ID
     */
    public String generateFreezeLogId() {
        return "FRZ" + snowflake.nextIdStr();
    }

    /**
     * 生成消息ID
     * 格式：MSG + 雪花ID
     * @return 消息ID
     */
    public String generateMessageId() {
        return "MSG" + snowflake.nextIdStr();
    }

    /**
     * 生成回调日志ID
     * 格式：CBL + 雪花ID
     * @return 回调日志ID
     */
    public String generateCallbackLogId() {
        return "CBL" + snowflake.nextIdStr();
    }

    /**
     * 生成Saga事务ID
     * 格式：SAGA + 雪花ID
     * @return Saga事务ID
     */
    public String generateSagaId() {
        return "SAGA" + snowflake.nextIdStr();
    }

    /**
     * 生成分片ID
     * 格式：{mainAccountId}_SHARD_{index}
     * @param mainAccountId 主账户ID
     * @param shardIndex 分片索引
     * @return 分片ID
     */
    public String generateShardId(String mainAccountId, int shardIndex) {
        return mainAccountId + "_SHARD_" + String.format("%02d", shardIndex);
    }
}
