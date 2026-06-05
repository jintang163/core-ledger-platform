package com.bank.core.account.config;

import com.bank.core.common.utils.DistributedLockUtil;
import com.bank.core.common.utils.IdempotentUtil;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class RedissonConfig {

    @Autowired
    private RedissonClient redissonClient;

    @PostConstruct
    public void init() {
        DistributedLockUtil.setRedissonClient(redissonClient);
        IdempotentUtil.setRedissonClient(redissonClient);
    }
}
