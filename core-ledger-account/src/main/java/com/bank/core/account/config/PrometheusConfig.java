package com.bank.core.account.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class PrometheusConfig {

    private final MeterRegistry meterRegistry;
    private final AtomicLong activeAccounts = new AtomicLong(0);

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
}
