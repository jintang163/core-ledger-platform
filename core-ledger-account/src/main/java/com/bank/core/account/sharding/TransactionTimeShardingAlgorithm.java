package com.bank.core.account.sharding;

import com.google.common.collect.Range;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * 交易时间按月分表算法
 * 
 * 用于交易表和交易分录表的表分片，根据交易时间（transaction_time）按月份分表。
 * 
 * 表命名规则：
 * - t_transaction_202501, t_transaction_202502, ..., t_transaction_202512
 * - t_transaction_entry_202501, t_transaction_entry_202502, ...
 * 
 * 设计要点：
 * 1. 按月分表，单表数据量可控，便于历史数据归档
 * 2. 支持时间范围查询，自动路由到相关月份表
 * 3. 支持未来月份表自动创建（需配合DDL自动创建或提前创建）
 * 
 * 分表优势：
 * - 冷热数据分离：历史数据可单独归档或迁移
 * - 查询性能：单表数据量小，索引效率高
 * - 运维方便：可按月份备份、恢复、清理数据
 */
public class TransactionTimeShardingAlgorithm implements StandardShardingAlgorithm<LocalDateTime> {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String TABLE_PREFIX = "";

    private Properties props;

    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<LocalDateTime> shardingValue) {
        LocalDateTime transactionTime = shardingValue.getValue();
        String suffix = transactionTime.format(MONTH_FORMATTER);
        String targetTable = shardingValue.getLogicTableName() + TABLE_PREFIX + "_" + suffix;
        
        for (String available : availableTargetNames) {
            if (available.equalsIgnoreCase(targetTable)) {
                return available;
            }
        }
        
        for (String available : availableTargetNames) {
            if (available.endsWith(suffix)) {
                return available;
            }
        }
        
        throw new IllegalArgumentException("No target table found for time: " + transactionTime 
                + ", available tables: " + availableTargetNames);
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, RangeShardingValue<LocalDateTime> shardingValue) {
        Range<LocalDateTime> range = shardingValue.getValueRange();
        LocalDateTime startTime = range.hasLowerBound() ? range.lowerEndpoint() : LocalDateTime.now().minusYears(1);
        LocalDateTime endTime = range.hasUpperBound() ? range.upperEndpoint() : LocalDateTime.now();
        
        List<String> result = new ArrayList<>();
        LocalDateTime current = startTime.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        
        while (!current.isAfter(endTime)) {
            String suffix = current.format(MONTH_FORMATTER);
            for (String available : availableTargetNames) {
                if (available.endsWith("_" + suffix)) {
                    result.add(available);
                }
            }
            current = current.plusMonths(1);
        }
        
        if (result.isEmpty()) {
            result.addAll(availableTargetNames);
        }
        
        return result;
    }

    @Override
    public Properties getProps() {
        return props;
    }

    @Override
    public void init(Properties props) {
        this.props = props;
    }

    @Override
    public String getType() {
        return "TRANSACTION_TIME_MONTH";
    }
}
