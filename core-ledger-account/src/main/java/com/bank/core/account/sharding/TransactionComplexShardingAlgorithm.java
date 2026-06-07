package com.bank.core.account.sharding;

import cn.hutool.core.date.DateUtil;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 交易表复合分片算法
 * 
 * 用于交易表和交易分录表的分片，采用复合分片键（account_id + transaction_time）。
 * 
 * 分片策略：
 * 1. 数据库分片：根据 account_id 哈希取模路由到不同数据库
 * 2. 数据表分片：根据 transaction_time 按月份分表（t_transaction_202506, t_transaction_202507, ...）
 * 
 * 设计要点：
 * 1. 复合分片键确保同一账户的交易数据按时间有序存储
 * 2. 按月分表支持历史数据归档和冷热分离
 * 3. 账户ID哈希确保数据均匀分布，避免热点问题
 * 4. 支持精确查询（account_id + transaction_time）直接路由到单表
 * 5. 支持范围查询（account_id + 时间范围）路由到相关月份表
 * 
 * 索引优化配合：
 * - 联合索引 (account_id, transaction_time) 支持高效的账户+时间范围查询
 * - 单表数据量控制在千万级，查询性能稳定
 */
public class TransactionComplexShardingAlgorithm implements ComplexKeysShardingAlgorithm<Comparable<?>> {

    private static final String DEFAULT_DATABASE_COUNT = "2";
    private static final String DATABASE_COUNT_KEY = "database-count";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private Properties props;
    private int databaseCount;

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, 
                                         ComplexKeysShardingValue<Comparable<?>> shardingValue) {
        Map<String, Collection<Comparable<?>>> columnNameAndShardingValuesMap = shardingValue.getColumnNameAndShardingValuesMap();
        
        String accountId = getAccountId(columnNameAndShardingValuesMap);
        LocalDateTime transactionTime = getTransactionTime(columnNameAndShardingValuesMap);
        
        if (accountId == null && transactionTime == null) {
            return availableTargetNames;
        }
        
        Set<String> results = new LinkedHashSet<>();
        
        if (isDatabaseTarget(availableTargetNames)) {
            String database = calculateDatabase(accountId);
            for (String available : availableTargetNames) {
                if (available.equalsIgnoreCase(database)) {
                    results.add(available);
                    break;
                }
            }
            if (results.isEmpty()) {
                results.addAll(availableTargetNames);
            }
        } else {
            Collection<String> tableSuffixes = calculateTableSuffix(transactionTime);
            String logicTableName = shardingValue.getLogicTableName();
            for (String suffix : tableSuffixes) {
                String targetTable = logicTableName + "_" + suffix;
                for (String available : availableTargetNames) {
                    if (available.equalsIgnoreCase(targetTable)) {
                        results.add(available);
                        break;
                    }
                }
            }
            if (results.isEmpty()) {
                results.addAll(availableTargetNames);
            }
        }
        
        return results;
    }

    private boolean isDatabaseTarget(Collection<String> availableTargetNames) {
        for (String name : availableTargetNames) {
            if (name.startsWith("ds")) {
                return true;
            }
        }
        return false;
    }

    private String getAccountId(Map<String, Collection<Comparable<?>>> columnNameAndShardingValuesMap) {
        Collection<Comparable<?>> accountIds = columnNameAndShardingValuesMap.get("account_id");
        if (accountIds != null && !accountIds.isEmpty()) {
            return accountIds.iterator().next().toString();
        }
        accountIds = columnNameAndShardingValuesMap.get("accountId");
        if (accountIds != null && !accountIds.isEmpty()) {
            return accountIds.iterator().next().toString();
        }
        return null;
    }

    private LocalDateTime getTransactionTime(Map<String, Collection<Comparable<?>>> columnNameAndShardingValuesMap) {
        Collection<Comparable<?>> transactionTimes = columnNameAndShardingValuesMap.get("transaction_time");
        if (transactionTimes != null && !transactionTimes.isEmpty()) {
            Object value = transactionTimes.iterator().next();
            if (value instanceof LocalDateTime) {
                return (LocalDateTime) value;
            }
            if (value instanceof String) {
                return LocalDateTime.parse((String) value);
            }
        }
        return null;
    }

    private String calculateDatabase(String accountId) {
        if (accountId == null) {
            return "ds0";
        }
        int hash = Math.abs(accountId.hashCode());
        int index = hash % databaseCount;
        return "ds" + index;
    }

    private Collection<String> calculateTableSuffix(LocalDateTime transactionTime) {
        if (transactionTime == null) {
            List<String> allMonths = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (int i = 0; i < 12; i++) {
                allMonths.add(now.minusMonths(i).format(MONTH_FORMATTER));
            }
            return allMonths;
        }
        return Collections.singletonList(transactionTime.format(MONTH_FORMATTER));
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
        return "TRANSACTION_COMPLEX";
    }
}
