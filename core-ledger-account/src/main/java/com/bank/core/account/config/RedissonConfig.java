package com.bank.core.account.config;

import com.bank.core.common.utils.DistributedLockUtil;
import com.bank.core.common.utils.IdempotentUtil;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * Redisson配置类
 * 
 * 负责将Spring容器管理的RedissonClient实例注入到静态工具类中，
 * 解决工具类无法直接使用@Autowired注入的问题。
 * 
 * 初始化时机：
 * - Spring容器启动完成后，Bean实例化并完成依赖注入后
 * - 通过@PostConstruct注解标记的init方法执行
 * 
 * 注入的工具类：
 * 1. DistributedLockUtil：分布式锁工具类
 * 2. IdempotentUtil：幂等性工具类
 */
@Configuration
public class RedissonConfig {

    /** Redisson客户端，由Spring Boot自动配置注入 */
    @Autowired
    private RedissonClient redissonClient;

    /**
     * 初始化方法，将RedissonClient注入到静态工具类
     * 在Bean初始化完成后自动执行
     */
    @PostConstruct
    public void init() {
        DistributedLockUtil.setRedissonClient(redissonClient);
        IdempotentUtil.setRedissonClient(redissonClient);
    }
}
