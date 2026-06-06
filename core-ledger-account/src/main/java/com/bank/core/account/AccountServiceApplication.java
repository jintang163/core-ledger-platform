package com.bank.core.account;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 账户服务启动类
 * 
 * 核心功能：
 * 1. 账户管理（开户、销户、冻结、解冻）
 * 2. 交易记账（支持双分录记账）
 * 3. 高并发处理（热点账户分片、缓冲记账、乐观锁重试）
 * 
 * 注解说明：
 * - @EnableDubbo：启用Dubbo RPC服务
 * - @EnableDiscoveryClient：启用服务注册发现（Nacos）
 * - @EnableScheduling：启用Spring定时任务（用于影子账户归并、缓冲记账处理）
 * - @SpringBootApplication：Spring Boot启动注解，指定扫描包
 * - @MapperScan：MyBatis Mapper接口扫描
 */
@EnableDubbo
@EnableDiscoveryClient
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.bank.core.account", "com.bank.core.common"})
@MapperScan("com.bank.core.account.mapper")
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
}
