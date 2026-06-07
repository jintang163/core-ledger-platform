package com.bank.core.account.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.Properties;

/**
 * 账户ID哈希分片算法
 * 
 * 用于账户表的数据库分片，根据账户ID（account_id）的哈希值路由到不同的数据库。
 * 
 * 设计要点：
 * 1. 使用 account_id 作为分片键，确保同一账户的数据落在同一个库中
 * 2. 哈希算法采用 String.hashCode() 取绝对值后取模
 * 3. 支持库数量动态配置（默认2个库）
 * 
 * 分片键选择理由：
 * - 账户ID是业务主键，全局唯一，分布均匀
 * - 账户查询通常以账户ID为条件，可直接路由到单库
 * - 关联查询（如交易流水）可通过账户ID关联，避免跨库join
 */
public class AccountIdHashShardingAlgorithm implements StandardShardingAlgorithm<String> {

    private static final String DEFAULT_DATABASE_COUNT = "2";
    private static final String DATABASE_COUNT_KEY = "database-count";

    private Properties props;
    private int databaseCount;

    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<String> shardingValue) {
        String accountId = shardingValue.getValue();
        int hash = Math.abs(accountId.hashCode());
        int index = hash % databaseCount;
        String targetDatabase = "ds" + index;
        for (String available : availableTargetNames) {
            if (available.equalsIgnoreCase(targetDatabase)) {
                return available;
            }
        }
        throw new IllegalArgumentException("No target database found for accountId: " + accountId);
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, RangeShardingValue<String> shardingValue) {
        return availableTargetNames;
    }

    @Override
    public Properties getProps() {
        return props;
    }

    @Override
    public void init(Properties props) {
        this.props = props;
        this.databaseCount = Integer.parseInt(props.getProperty(DATABASE_COUNT_KEY, DEFAULT_DATABASE_COUNT));
    }

    @Override
    public String getType() {
        return "ACCOUNT_ID_HASH";
    }
}
