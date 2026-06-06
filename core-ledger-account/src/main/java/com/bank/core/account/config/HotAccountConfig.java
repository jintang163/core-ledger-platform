package com.bank.core.account.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 热点账户与高并发配置
 * 提供热点账户分片、缓冲记账等功能的配置开关
 *
 * 配置示例（application.yml）：
 * account:
 *   hot:
 *     enabled: true                    # 是否启用热点账户功能
 *     sharding-enabled: true           # 是否启用热点账户分片
 *     buffer-enabled: true             # 是否启用缓冲记账
 *     account-lock-enabled: true       # 是否启用账户级分布式锁
 *     optimistic-retry-enabled: true   # 是否启用乐观锁重试
 *     default-shard-count: 10          # 默认分片数量
 *     shard-merge-cron: "0 0 2 * * ?"  # 影子账户归并定时任务Cron
 *   buffer:
 *     process-interval: 1000           # 缓冲记账处理间隔（毫秒）
 *     batch-size: 100                  # 缓冲记账批量处理大小
 *     max-delay: 5                     # 缓冲记账最大延迟（秒）
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "account")
public class HotAccountConfig {

    private HotConfig hot = new HotConfig();
    private BufferConfig buffer = new BufferConfig();

    @Data
    public static class HotConfig {
        /** 是否启用热点账户功能总开关 */
        private boolean enabled = true;

        /** 是否启用热点账户分片 */
        private boolean shardingEnabled = true;

        /** 是否启用缓冲记账 */
        private boolean bufferEnabled = true;

        /** 是否启用账户级分布式锁 */
        private boolean accountLockEnabled = true;

        /** 是否启用乐观锁重试 */
        private boolean optimisticRetryEnabled = true;

        /** 默认分片数量 */
        private int defaultShardCount = 10;

        /** 影子账户归并定时任务Cron */
        private String shardMergeCron = "0 0 2 * * ?";
    }

    @Data
    public static class BufferConfig {
        /** 缓冲记账处理间隔（毫秒） */
        private long processInterval = 1000L;

        /** 缓冲记账批量处理大小 */
        private int batchSize = 100;

        /** 缓冲记账最大延迟（秒） */
        private int maxDelay = 5;
    }
}
