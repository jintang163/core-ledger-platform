package com.bank.core.account.listener;

import brave.Span;
import brave.Tracer;
import com.bank.core.account.config.TracingConfig;
import com.bank.core.account.scheduler.MonitorScheduler;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.core.context.RootContext;
import io.seata.core.event.GlobalTransactionEvent;
import io.seata.core.model.GlobalStatus;
import io.seata.spring.event.GlobalTransactionEventListener;
import io.seata.tm.api.transaction.TransactionHook;
import io.seata.tm.api.transaction.TransactionHookAdapter;
import io.seata.tm.api.transaction.TransactionHookManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeataTransactionListener implements ApplicationListener<ContextRefreshedEvent>, GlobalTransactionEventListener {

    private final MonitorScheduler monitorScheduler;
    private final Tracer tracer;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            new NamedThreadFactory("seata-tx-monitor", 1, true));
    private final Map<String, Long> transactionStartTimeMap = new ConcurrentHashMap<>();
    private final Map<String, Span> transactionSpanMap = new ConcurrentHashMap<>();

    private static final long TIMEOUT_CHECK_INTERVAL = 30;
    private static final long DEFAULT_TX_TIMEOUT = 60000;

    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(this::checkTimeoutTransactions,
                TIMEOUT_CHECK_INTERVAL, TIMEOUT_CHECK_INTERVAL, TimeUnit.SECONDS);

        registerTransactionHook();
        log.info("Seata事务监听器初始化完成，超时检查间隔: {}秒", TIMEOUT_CHECK_INTERVAL);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("Spring容器初始化完成，Seata事务监听器已就绪");
    }

    @Override
    public void onGlobalTransactionEvent(GlobalTransactionEvent event) {
        String xid = event.getXid();
        GlobalStatus status = event.getStatus();
        String transactionName = event.getTransactionName();

        log.info("收到Seata全局事务事件, xid={}, name={}, status={}", xid, transactionName, status);

        handleGlobalTransactionEvent(xid, transactionName, status, event.getBeginTime(), event.getEndTime());
    }

    @io.seata.common.eventbus.Subscribe
    public void onGlobalTransaction(GlobalTransactionEvent event) {
        String xid = event.getXid();
        GlobalStatus status = event.getStatus();
        String transactionName = event.getTransactionName();

        log.debug("Seata EventBus收到全局事务事件, xid={}, name={}, status={}", xid, transactionName, status);

        handleGlobalTransactionEvent(xid, transactionName, status, event.getBeginTime(), event.getEndTime());
    }

    private void handleGlobalTransactionEvent(String xid, String transactionName, GlobalStatus status,
                                              Long beginTime, Long endTime) {
        if (xid == null) {
            return;
        }

        try {
            switch (status) {
                case Begin:
                    handleTransactionBegin(xid, transactionName, beginTime);
                    break;
                case Committed:
                    handleTransactionCommitted(xid, transactionName, endTime);
                    break;
                case Rollbacked:
                    handleTransactionRollbacked(xid, transactionName, endTime);
                    break;
                case TimeoutRollbacking:
                    handleTransactionTimeout(xid, transactionName, beginTime, "开始超时回滚");
                    break;
                case TimeoutRollbacked:
                    handleTransactionTimeout(xid, transactionName, endTime, "超时回滚完成");
                    break;
                case CommitFailed:
                    handleTransactionFailed(xid, transactionName, endTime, "提交失败");
                    break;
                case RollbackFailed:
                    handleTransactionFailed(xid, transactionName, endTime, "回滚失败");
                    break;
                case Rollbacking:
                    log.debug("全局事务回滚中, xid={}, name={}", xid, transactionName);
                    break;
                case Committing:
                    log.debug("全局事务提交中, xid={}, name={}", xid, transactionName);
                    break;
                default:
                    log.debug("全局事务状态变更, xid={}, name={}, status={}", xid, transactionName, status);
                    break;
            }
        } catch (Exception e) {
            log.error("处理Seata事务事件失败, xid={}", xid, e);
        }
    }

    private void handleTransactionBegin(String xid, String transactionName, Long beginTime) {
        long startTime = beginTime != null ? beginTime : System.currentTimeMillis();
        transactionStartTimeMap.put(xid, startTime);

        Span span = tracer.nextSpan().name("seata-global-tx:" + transactionName).start();
        span.tag("xid", xid);
        span.tag("transaction.name", transactionName);
        span.tag("phase", "begin");
        span.tag("component", "seata");
        span.tag("transaction.type", "TCC/Saga");
        span.start(startTime);
        transactionSpanMap.put(xid, span);

        TracingConfig.setTransactionTag(null, xid);

        log.info("全局事务开始, xid={}, name={}, startTime={}", xid, transactionName, startTime);
    }

    private void handleTransactionCommitted(String xid, String transactionName, Long endTime) {
        long end = endTime != null ? endTime : System.currentTimeMillis();
        Long startTime = transactionStartTimeMap.remove(xid);
        long duration = startTime != null ? (end - startTime) : 0;

        Span span = transactionSpanMap.remove(xid);
        if (span != null) {
            span.tag("status", "success");
            span.tag("phase", "committed");
            span.tag("duration.ms", String.valueOf(duration));
            span.finish(end);
        }

        monitorScheduler.getPrometheusConfig().recordDistributedTransactionSuccess();

        TracingConfig.clearTransactionTag();

        log.info("全局事务提交成功, xid={}, name={}, duration={}ms", xid, transactionName, duration);
    }

    private void handleTransactionRollbacked(String xid, String transactionName, Long endTime) {
        long end = endTime != null ? endTime : System.currentTimeMillis();
        Long startTime = transactionStartTimeMap.remove(xid);
        long duration = startTime != null ? (end - startTime) : 0;

        Span span = transactionSpanMap.remove(xid);
        if (span != null) {
            span.tag("status", "rollback");
            span.tag("phase", "rollbacked");
            span.tag("duration.ms", String.valueOf(duration));
            span.finish(end);
        }

        monitorScheduler.getPrometheusConfig().recordDistributedTransactionFailure();

        TracingConfig.clearTransactionTag();

        log.warn("全局事务回滚, xid={}, name={}, duration={}ms", xid, transactionName, duration);
    }

    private void handleTransactionTimeout(String xid, String transactionName, Long time, String message) {
        long eventTime = time != null ? time : System.currentTimeMillis();
        Long startTime = transactionStartTimeMap.get(xid);
        long duration = startTime != null ? (eventTime - startTime) : 0;

        Span span = transactionSpanMap.get(xid);
        if (span != null) {
            span.tag("status", "timeout");
            span.tag("timeout.message", message);
            span.tag("timeout.duration.ms", String.valueOf(duration));
            span.annotate(time, message);
        }

        monitorScheduler.recordSeataTransactionTimeout(xid);
        monitorScheduler.getPrometheusConfig().incrementDistributedTransactionTimeouts();

        log.error("全局事务超时, xid={}, name={}, message={}, duration={}ms",
                xid, transactionName, message, duration);
    }

    private void handleTransactionFailed(String xid, String transactionName, Long endTime, String message) {
        long end = endTime != null ? endTime : System.currentTimeMillis();
        Long startTime = transactionStartTimeMap.remove(xid);
        long duration = startTime != null ? (end - startTime) : 0;

        Span span = transactionSpanMap.remove(xid);
        if (span != null) {
            span.tag("status", "failed");
            span.tag("error.message", message);
            span.tag("duration.ms", String.valueOf(duration));
            span.error(new RuntimeException(message));
            span.finish(end);
        }

        monitorScheduler.getPrometheusConfig().recordDistributedTransactionFailure();

        TracingConfig.clearTransactionTag();

        log.error("全局事务执行失败, xid={}, name={}, message={}, duration={}ms",
                xid, transactionName, message, duration);
    }

    private void checkTimeoutTransactions() {
        try {
            long now = System.currentTimeMillis();
            transactionStartTimeMap.forEach((xid, startTime) -> {
                long duration = now - startTime;
                if (duration > DEFAULT_TX_TIMEOUT) {
                    log.warn("检测到可能超时的全局事务, xid={}, duration={}ms, timeout={}ms",
                            xid, duration, DEFAULT_TX_TIMEOUT);
                }
            });
        } catch (Exception e) {
            log.error("检查Seata事务超时失败", e);
        }
    }

    private void registerTransactionHook() {
        TransactionHook hook = new TransactionHookAdapter() {
            @Override
            public void beforeBegin() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.debug("事务钩子 - 开始前, xid={}", xid);
                }
            }

            @Override
            public void afterBegin() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.debug("事务钩子 - 开始后, xid={}", xid);
                    Span span = tracer.currentSpan();
                    if (span != null) {
                        span.tag("seata.xid", xid);
                    }
                }
            }

            @Override
            public void beforeCommit() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.debug("事务钩子 - 提交前, xid={}", xid);
                }
            }

            @Override
            public void afterCommit() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.debug("事务钩子 - 提交后, xid={}", xid);
                    handleTransactionCommitted(xid, "hook-transaction", System.currentTimeMillis());
                }
            }

            @Override
            public void beforeRollback() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.debug("事务钩子 - 回滚前, xid={}", xid);
                }
            }

            @Override
            public void afterRollback() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.debug("事务钩子 - 回滚后, xid={}", xid);
                    handleTransactionRollbacked(xid, "hook-transaction", System.currentTimeMillis());
                }
            }

            @Override
            public void afterCompletion() {
                String xid = RootContext.getXID();
                if (xid != null) {
                    log.debug("事务钩子 - 完成后, xid={}", xid);
                    transactionStartTimeMap.remove(xid);
                    transactionSpanMap.remove(xid);
                    TracingConfig.clearTransactionTag();
                }
            }
        };

        TransactionHookManager.registerHook(hook);
        log.info("Seata事务钩子注册完成");
    }
}
