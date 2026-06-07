package com.bank.core.account.scheduler;

import com.bank.core.account.config.PrometheusConfig;
import com.bank.core.account.entity.Account;
import com.bank.core.account.mapper.AccountMapper;
import com.bank.core.account.mapper.AccountBufferLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorScheduler {

    private final PrometheusConfig prometheusConfig;
    private final AccountMapper accountMapper;
    private final AccountBufferLogMapper accountBufferLogMapper;
    private final RedissonClient redissonClient;

    private static final int MAX_FREEZE_HOURS = 24;
    private static final String SEATA_TIMEOUT_KEY = "seata:transaction:timeout:count";

    @Scheduled(fixedDelay = 30000)
    public void checkNegativeBalanceAccounts() {
        try {
            long count = accountMapper.countNegativeBalanceAccounts();
            prometheusConfig.setNegativeBalanceAccounts(count);
            if (count > 0) {
                log.warn("检测到 {} 个账户余额为负数", count);
            }
        } catch (Exception e) {
            log.error("检查负数余额账户失败", e);
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void checkAbnormalFrozenAccounts() {
        try {
            LocalDateTime thresholdTime = LocalDateTime.now().minusHours(MAX_FREEZE_HOURS);
            List<Account> abnormalAccounts = accountMapper.selectAbnormalFrozenAccounts(thresholdTime);
            if (!abnormalAccounts.isEmpty()) {
                log.warn("检测到 {} 个账户冻结时间超过 {} 小时", abnormalAccounts.size(), MAX_FREEZE_HOURS);
                for (Account account : abnormalAccounts) {
                    log.warn("异常冻结账户: accountId={}, freezeTime={}",
                            account.getAccountId(), account.getFreezeTime());
                }
            }
        } catch (Exception e) {
            log.error("检查异常冻结账户失败", e);
        }
    }

    @Scheduled(fixedDelay = 10000)
    public void checkMessageQueueLag() {
        try {
            long pendingCount = accountBufferLogMapper.countPendingLogs();
            prometheusConfig.setMessageQueueLag(pendingCount);

            if (pendingCount > 1000) {
                log.warn("消息队列（缓冲流水）堆积严重，当前待处理数量: {}", pendingCount);
            }

            RMap<String, Integer> timeoutMap = redissonClient.getMap(SEATA_TIMEOUT_KEY);
            if (!timeoutMap.isEmpty()) {
                int totalTimeout = timeoutMap.values().stream().mapToInt(Integer::intValue).sum();
                for (int i = 0; i < totalTimeout; i++) {
                    prometheusConfig.incrementDistributedTransactionTimeouts();
                }
                timeoutMap.clear();
            }
        } catch (Exception e) {
            log.error("检查消息队列堆积失败", e);
        }
    }

    public void recordSeataTransactionTimeout(String globalTxId) {
        try {
            RMap<String, Integer> timeoutMap = redissonClient.getMap(SEATA_TIMEOUT_KEY);
            timeoutMap.merge(globalTxId, 1, Integer::sum);
            log.error("Seata分布式事务超时, globalTxId={}", globalTxId);
        } catch (Exception e) {
            log.error("记录Seata事务超时失败", e);
        }
    }
}
