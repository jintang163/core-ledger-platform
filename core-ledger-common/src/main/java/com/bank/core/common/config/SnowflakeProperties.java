package com.bank.core.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 雪花算法ID生成器配置
 * 
 * 支持通过配置文件动态调整雪花算法参数，避免多实例部署时的ID冲突。
 * 
 * 配置示例（application.yml）：
 * snowflake:
 *   worker-id: 1
 *   data-center-id: 1
 * 
 * 注意：
 * - worker-id 范围：0-31，每个部署实例必须唯一
 * - data-center-id 范围：0-31，每个数据中心必须唯一
 * - 两者组合最多支持 32*32=1024 个实例
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "snowflake")
public class SnowflakeProperties {

    /**
     * 工作机器ID（0-31）
     * 每个部署实例必须分配唯一的workerId
     */
    private Long workerId = 1L;

    /**
     * 数据中心ID（0-31）
     * 跨数据中心部署时使用，同一数据中心实例共享相同dataCenterId
     */
    private Long dataCenterId = 1L;
}
