package com.bank.core.account.sharding;

import org.apache.shardingsphere.sharding.spi.ShardingAlgorithm;

import java.util.Properties;

/**
 * 分片算法工厂
 * 
 * 用于加载和注册自定义分片算法到 ShardingSphere。
 * ShardingSphere 5.x 版本通过 SPI 机制自动发现分片算法。
 * 
 * 自定义算法类型：
 * 1. ACCOUNT_ID_HASH - 账户ID哈希分片（用于账户表数据库分片
 * 2. TRANSACTION_COMPLEX - 交易复合分片（用于交易表复合分片）
 * 3. TRANSACTION_TIME_MONTH - 交易时间按月分表（用于交易表表分片）
 */
public final class ShardingAlgorithmFactory {

    private ShardingAlgorithmFactory() {
    }

    /**
     * 创建账户ID哈希分片算法
     */
    public static ShardingAlgorithm createAccountIdHashAlgorithm(int databaseCount) {
        AccountIdHashShardingAlgorithm algorithm = new AccountIdHashShardingAlgorithm();
        Properties props = new Properties();
        props.setProperty("database-count", String.valueOf(databaseCount));
        algorithm.init(props);
        return algorithm;
    }

    /**
     * 创建交易复合分片算法
     */
    public static ShardingAlgorithm createTransactionComplexAlgorithm(int databaseCount) {
        TransactionComplexShardingAlgorithm algorithm = new TransactionComplexShardingAlgorithm();
        Properties props = new Properties();
        props.setProperty("database-count", String.valueOf(databaseCount));
        algorithm.init(props);
        return algorithm;
    }

    /**
     * 创建交易时间按月分表算法
     */
    public static ShardingAlgorithm createTransactionTimeAlgorithm() {
        return new TransactionTimeShardingAlgorithm();
    }
}
