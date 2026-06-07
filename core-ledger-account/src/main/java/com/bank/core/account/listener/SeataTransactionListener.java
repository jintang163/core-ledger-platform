package com.bank.core.account.listener;

import com.bank.core.account.scheduler.MonitorScheduler;
import io.seata.core.event.GlobalTransactionEvent;
import io.seata.core.event.TransactionEvent;
import io.seata.core.model.GlobalStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeataTransactionListener {

    private final MonitorScheduler monitorScheduler;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static final long TIMEOUT_CHECK_INTERVAL = 30;
    private static final long DEFAULT_TX_TIMEOUT = 60000;

    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::checkTimeoutTransactions,
                TIMEOUT_CHECK_INTERVAL, TIMEOUT_CHECK_INTERVAL, TimeUnit.SECONDS);
        log.info("Seata事务监听器初始化完成，超时检查间隔: {}秒", TIMEOUT_CHECK_INTERVAL);
    }

    private void checkTimeoutTransactions() {
        try {
            log.debug("开始检查Seata分布式事务超时情况");
        } catch (Exception e) {
            log.error("检查Seata事务超时失败", e);
        }
    }

    public void onGlobalTransactionEvent(GlobalTransactionEvent event) {
        String globalTxId = event.getXid();
        GlobalStatus status = event.getStatus();

        log.info("收到Seata全局事务事件, globalTxId={}, status={}", globalTxId, status);

        switch (status) {
            case Begin:
                log.info("全局事务开始, globalTxId={}", globalTxId);
                break;
            case Committed:
                log.info("全局事务提交成功, globalTxId={}", globalTxId);
                break;
            case Rollbacked:
                log.warn("全局事务回滚, globalTxId={}", globalTxId);
                break;
            case TimeoutRollbacking:
            case TimeoutRollbacked:
                log.error("全局事务超时回滚, globalTxId={}, status={}", globalTxId, status);
                monitorScheduler.recordSeataTransactionTimeout(globalTxId);
                break;
            case CommitFailed:
            case RollbackFailed:
                log.error("全局事务执行失败, globalTxId={}, status={}", globalTxId, status);
                break;
            default:
                log.debug("全局事务状态变更, globalTxId={}, status={}", globalTxId, status);
                break;
        }
    }

    public void onTransactionEvent(TransactionEvent event) {
        log.debug("收到Seata事务事件, branchId={}, status={}", event.getBranchId(), event.getStatus());
    }
}
