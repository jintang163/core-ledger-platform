package com.bank.core.account.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.ToDoubleFunction;

@Configuration
public class PrometheusConfig {

    private final MeterRegistry meterRegistry;
    private final AtomicLong activeAccounts = new AtomicLong(0);
    private final AtomicLong negativeBalanceAccounts = new AtomicLong(0);
    private final AtomicLong frozenAccountExceptions = new AtomicLong(0);
    private final AtomicLong distributedTransactionTimeouts = new AtomicLong(0);
    private final AtomicLong messageQueueLag = new AtomicLong(0);
    private final Map<String, AtomicLong> gauges = new ConcurrentHashMap<>();

    private Counter transactionQpsCounter;
    private Counter transactionSuccessCounter;
    private Counter transactionFailureCounter;
    private Counter hotAccountConflictCounter;
    private Counter distributedTransactionSuccessCounter;
    private Counter distributedTransactionFailureCounter;
    private Timer transactionLatencyTimer;
    private Timer transferLatencyTimer;
    private Timer hotAccountMergeTimer;
    private DistributionSummary transactionAmountSummary;

    public PrometheusConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    @PostConstruct
    public void initMetrics() {
        Gauge.builder("account.active.count", activeAccounts, AtomicLong::get)
                .description("活跃账户数")
                .register(meterRegistry);

        Gauge.builder("account.negative_balance.count", negativeBalanceAccounts, AtomicLong::get)
                .description("余额为负数的账户数")
                .register(meterRegistry);

        Gauge.builder("account.frozen_exception.count", frozenAccountExceptions, AtomicLong::get)
                .description("账户冻结异常次数")
                .register(meterRegistry);

        Gauge.builder("distributed.transaction.timeout.count", distributedTransactionTimeouts, AtomicLong::get)
                .description("分布式事务超时次数")
                .register(meterRegistry);

        Gauge.builder("message.queue.lag", messageQueueLag, AtomicLong::get)
                .description("消息队列堆积数量")
                .register(meterRegistry);

        Counter.builder("account.create.total")
                .description("账户创建总数")
                .register(meterRegistry);

        Counter.builder("account.freeze.total")
                .description("账户冻结总数")
                .register(meterRegistry);

        Counter.builder("account.unfreeze.total")
                .description("账户解冻总数")
                .register(meterRegistry);

        Counter.builder("account.close.total")
                .description("账户销户总数")
                .register(meterRegistry);

        transactionQpsCounter = Counter.builder("transaction.request.total")
                .description("交易请求总数")
                .tag("type", "transaction")
                .register(meterRegistry);

        transactionSuccessCounter = Counter.builder("transaction.success.total")
                .description("交易成功总数")
                .tag("type", "transaction")
                .register(meterRegistry);

        transactionFailureCounter = Counter.builder("transaction.failure.total")
                .description("交易失败总数")
                .tag("type", "transaction")
                .register(meterRegistry);

        hotAccountConflictCounter = Counter.builder("hot.account.conflict.total")
                .description("热点账户并发冲突总数")
                .register(meterRegistry);

        distributedTransactionSuccessCounter = Counter.builder("distributed.transaction.success.total")
                .description("分布式事务成功总数")
                .tag("pattern", "tcc_saga")
                .register(meterRegistry);

        distributedTransactionFailureCounter = Counter.builder("distributed.transaction.failure.total")
                .description("分布式事务失败总数")
                .tag("pattern", "tcc_saga")
                .register(meterRegistry);

        transactionLatencyTimer = Timer.builder("transaction.latency")
                .description("交易请求延迟")
                .tag("type", "transaction")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .distributionStatisticExpiry(Duration.ofMinutes(10))
                .register(meterRegistry);

        transferLatencyTimer = Timer.builder("transfer.latency")
                .description("转账请求延迟")
                .tag("type", "transfer")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .distributionStatisticExpiry(Duration.ofMinutes(10))
                .register(meterRegistry);

        hotAccountMergeTimer = Timer.builder("hot.account.merge.latency")
                .description("热点账户归并延迟")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(meterRegistry);

        transactionAmountSummary = DistributionSummary.builder("transaction.amount")
                .description("交易金额分布")
                .baseUnit("yuan")
                .publishPercentiles(0.5, 0.75, 0.9, 0.95, 0.99)
                .register(meterRegistry);
    }

    public void incrementActiveAccounts() {
        activeAccounts.incrementAndGet();
    }

    public void decrementActiveAccounts() {
        activeAccounts.decrementAndGet();
    }

    public void incrementCounter(String name) {
        Counter counter = meterRegistry.counter(name);
        counter.increment();
    }

    public void recordTransactionQps() {
        transactionQpsCounter.increment();
    }

    public void recordTransactionSuccess() {
        transactionSuccessCounter.increment();
    }

    public void recordTransactionFailure() {
        transactionFailureCounter.increment();
    }

    public void recordHotAccountConflict() {
        hotAccountConflictCounter.increment();
    }

    public void recordDistributedTransactionSuccess() {
        distributedTransactionSuccessCounter.increment();
    }

    public void recordDistributedTransactionFailure() {
        distributedTransactionFailureCounter.increment();
    }

    public void recordTransactionLatency(Runnable runnable) {
        transactionLatencyTimer.record(runnable);
    }

    public <T> T recordTransactionLatency(java.util.function.Supplier<T> supplier) {
        return transactionLatencyTimer.record(supplier);
    }

    public void recordTransferLatency(Runnable runnable) {
        transferLatencyTimer.record(runnable);
    }

    public <T> T recordTransferLatency(java.util.function.Supplier<T> supplier) {
        return transferLatencyTimer.record(supplier);
    }

    public void recordHotAccountMergeLatency(Runnable runnable) {
        hotAccountMergeTimer.record(runnable);
    }

    public void recordTransactionAmount(java.math.BigDecimal amount) {
        transactionAmountSummary.record(amount.doubleValue());
    }

    public void setNegativeBalanceAccounts(long count) {
        negativeBalanceAccounts.set(count);
    }

    public void incrementFrozenAccountExceptions() {
        frozenAccountExceptions.incrementAndGet();
    }

    public void incrementDistributedTransactionTimeouts() {
        distributedTransactionTimeouts.incrementAndGet();
    }

    public void setMessageQueueLag(long lag) {
        messageQueueLag.set(lag);
    }

    public void registerGauge(String name, String description, AtomicLong value) {
        gauges.put(name, value);
        Gauge.builder(name, value, AtomicLong::get)
                .description(description)
                .register(meterRegistry);
    }

    public double getTransactionFailureRate() {
        double total = transactionQpsCounter.count();
        if (total == 0) return 0.0;
        return transactionFailureCounter.count() / total;
    }

    public double getDistributedTransactionFailureRate() {
        double total = distributedTransactionSuccessCounter.count() + distributedTransactionFailureCounter.count();
        if (total == 0) return 0.0;
        return distributedTransactionFailureCounter.count() / total;
    }
}
