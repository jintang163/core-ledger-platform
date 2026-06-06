package com.bank.core.account.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 热点账户与高并发配置
 *
 * 核心功能：
 * 1. 提供热点账户分片、缓冲记账等功能的配置开关
 * 2. 通过@ConfigurationProperties注解自动绑定application.yml配置
 * 3. 支持运行时动态调整配置（需结合Nacos/Apollo等配置中心）
 *
 * 设计要点：
 * - 使用嵌套静态类（HotConfig、BufferConfig）组织配置，结构清晰
 * - 所有配置项都设置合理的默认值，零配置即可运行
 * - 配置前缀为"account"，便于与其他模块配置区分
 * - 支持通过Spring Cloud配置中心实现动态配置更新
 *
 * 配置示例（application.yml）：
 * account:
 *   hot:
 *     enabled: true                    # 是否启用热点账户功能总开关
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
 *
 * 配置说明：
 * - 热点账户分片：将高并发账户拆分为多个影子账户，分散写入压力
 * - 缓冲记账：高并发场景下先记流水，后异步批量更新余额，牺牲短暂一致性换取高吞吐
 * - 账户级分布式锁：细粒度锁，防止同一账户并发更新冲突
 * - 乐观锁重试：版本号更新失败时自动重试，提高并发场景下的成功率
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "account")
public class HotAccountConfig {

    private HotConfig hot = new HotConfig();
    private BufferConfig buffer = new BufferConfig();

    /**
     * 热点账户配置
     * 包含热点账户功能的总开关、分片策略、归并策略等配置
     */
    @Data
    public static class HotConfig {
        /**
         * 是否启用热点账户功能总开关
         * 关闭后所有热点账户相关功能（分片、缓冲）都将失效
         * 默认：true
         */
        private boolean enabled = true;

        /**
         * 是否启用热点账户分片
         * 启用后，热点账户会被拆分为多个影子账户，分散并发写入压力
         * 默认：true
         */
        private boolean shardingEnabled = true;

        /**
         * 是否启用缓冲记账
         * 启用后，高并发场景下先记录流水，后异步批量更新余额
         * 默认：true
         */
        private boolean bufferEnabled = true;

        /**
         * 是否启用账户级分布式锁
         * 启用后，账户余额更新时会先获取分布式锁，防止并发冲突
         * 默认：true
         */
        private boolean accountLockEnabled = true;

        /**
         * 是否启用乐观锁重试
         * 启用后，版本号更新失败时会自动重试，提高成功率
         * 默认：true
         */
        private boolean optimisticRetryEnabled = true;

        /**
         * 默认分片数量
         * 创建热点账户分片时如果未指定分片数量，使用此默认值
         * 建议根据实际并发压力调整，一般设为CPU核心数的2-4倍
         * 默认：10
         */
        private int defaultShardCount = 10;

        /**
         * 影子账户归并定时任务Cron表达式
         * 定时将影子账户余额归并到主账户
         * 默认：每天凌晨2点执行（"0 0 2 * * ?"）
         * 建议设置在业务低峰期执行
         */
        private String shardMergeCron = "0 0 2 * * ?";
    }

    /**
     * 缓冲记账配置
     * 包含缓冲记账的处理间隔、批量大小、最大延迟等配置
     */
    @Data
    public static class BufferConfig {
        /**
         * 缓冲记账处理间隔（毫秒）
         * 定时任务每隔多久处理一次待处理的缓冲流水
         * 间隔越小，实时性越高，但数据库压力越大
         * 默认：1000毫秒（1秒）
         */
        private long processInterval = 1000L;

        /**
         * 缓冲记账批量处理大小
         * 每次定时任务处理多少条缓冲流水
         * 批量越大，处理效率越高，但单笔延迟可能增加
         * 默认：100
         */
        private int batchSize = 100;

        /**
         * 缓冲记账最大延迟（秒）
         * 缓冲流水从创建到处理完成的最大预期延迟
         * 用于SLI监控和告警阈值设置
         * 默认：5秒
         */
        private int maxDelay = 5;
    }
}
