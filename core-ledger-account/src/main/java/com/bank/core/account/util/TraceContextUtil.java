package com.bank.core.account.util;

import brave.Span;
import brave.Tracer;
import brave.propagation.TraceContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.CallableWrapper;
import org.apache.skywalking.apm.toolkit.trace.RunnableWrapper;
import org.apache.skywalking.apm.toolkit.trace.SupplierWrapper;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

@Slf4j
@Component
public class TraceContextUtil {

    @Resource
    private Tracer tracer;

    private static final String TRACE_ID_KEY = "traceId";
    private static final String SPAN_ID_KEY = "spanId";
    private static final String TRANSACTION_ID_KEY = "transactionId";
    private static final String GLOBAL_TX_ID_KEY = "globalTxId";

    public String getTraceId() {
        String skywalkingTraceId = TraceContext.traceId();
        if (skywalkingTraceId != null && !"N/A".equals(skywalkingTraceId)) {
            return skywalkingTraceId;
        }
        TraceContext context = brave.propagation.ThreadLocalSpan.current().context();
        return context != null ? context.traceIdString() : null;
    }

    public String getSpanId() {
        TraceContext context = brave.propagation.ThreadLocalSpan.current().context();
        return context != null ? context.spanIdString() : null;
    }

    public void setTransactionTag(String transactionId) {
        Span span = tracer.currentSpan();
        if (span != null) {
            span.tag(TRANSACTION_ID_KEY, transactionId);
        }
        MDC.put(TRANSACTION_ID_KEY, transactionId);
        TraceContext.putCorrelation(TRANSACTION_ID_KEY, transactionId);
    }

    public void setGlobalTxTag(String globalTxId) {
        Span span = tracer.currentSpan();
        if (span != null) {
            span.tag(GLOBAL_TX_ID_KEY, globalTxId);
        }
        MDC.put(GLOBAL_TX_ID_KEY, globalTxId);
        TraceContext.putCorrelation(GLOBAL_TX_ID_KEY, globalTxId);
    }

    public void setCustomTag(String key, String value) {
        Span span = tracer.currentSpan();
        if (span != null) {
            span.tag(key, value);
        }
        MDC.put(key, value);
        TraceContext.putCorrelation(key, value);
    }

    public void annotate(String value) {
        Span span = tracer.currentSpan();
        if (span != null) {
            span.annotate(value);
        }
    }

    public void annotate(long timestamp, String value) {
        Span span = tracer.currentSpan();
        if (span != null) {
            span.annotate(timestamp, value);
        }
    }

    public void recordError(Throwable throwable) {
        Span span = tracer.currentSpan();
        if (span != null) {
            span.error(throwable);
        }
    }

    public Span createSpan(String name) {
        return tracer.nextSpan().name(name).start();
    }

    public Span createChildSpan(String name, TraceContext parent) {
        return tracer.newChild(parent).name(name).start();
    }

    public void closeSpan(Span span) {
        if (span != null) {
            span.finish();
        }
    }

    public <T> T executeWithNewSpan(String spanName, Supplier<T> supplier) {
        Span span = createSpan(spanName);
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            return supplier.get();
        } catch (Exception e) {
            span.error(e);
            throw e;
        } finally {
            span.finish();
        }
    }

    public void executeWithNewSpan(String spanName, Runnable runnable) {
        Span span = createSpan(spanName);
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            runnable.run();
        } catch (Exception e) {
            span.error(e);
            throw e;
        } finally {
            span.finish();
        }
    }

    public Runnable wrap(Runnable runnable) {
        Map<String, String> context = getCurrentContext();
        return RunnableWrapper.of(() -> {
            try {
                restoreContext(context);
                runnable.run();
            } finally {
                clearContext();
            }
        });
    }

    public <T> Callable<T> wrap(Callable<T> callable) {
        Map<String, String> context = getCurrentContext();
        return CallableWrapper.of(() -> {
            try {
                restoreContext(context);
                return callable.call();
            } finally {
                clearContext();
            }
        });
    }

    public <T> Supplier<T> wrap(Supplier<T> supplier) {
        Map<String, String> context = getCurrentContext();
        return SupplierWrapper.of(() -> {
            try {
                restoreContext(context);
                return supplier.get();
            } finally {
                clearContext();
            }
        });
    }

    public Map<String, String> getCurrentContext() {
        TraceContext traceContext = brave.propagation.ThreadLocalSpan.current().context();
        Map<String, String> context = new java.util.HashMap<>();
        if (traceContext != null) {
            context.put(TRACE_ID_KEY, traceContext.traceIdString());
            context.put(SPAN_ID_KEY, traceContext.spanIdString());
        }
        String transactionId = MDC.get(TRANSACTION_ID_KEY);
        if (transactionId != null) {
            context.put(TRANSACTION_ID_KEY, transactionId);
        }
        String globalTxId = MDC.get(GLOBAL_TX_ID_KEY);
        if (globalTxId != null) {
            context.put(GLOBAL_TX_ID_KEY, globalTxId);
        }
        return context;
    }

    public void restoreContext(Map<String, String> context) {
        if (context != null) {
            context.forEach(MDC::put);
            context.forEach(TraceContext::putCorrelation);
        }
    }

    public void clearContext() {
        MDC.remove(TRACE_ID_KEY);
        MDC.remove(SPAN_ID_KEY);
        MDC.remove(TRANSACTION_ID_KEY);
        MDC.remove(GLOBAL_TX_ID_KEY);
    }

    public boolean isTracingActive() {
        return getTraceId() != null;
    }

    public void logTraceInfo(String message) {
        String traceId = getTraceId();
        String spanId = getSpanId();
        log.info("{} [traceId={}, spanId={}]", message, traceId, spanId);
    }

    public void logTraceError(String message, Throwable throwable) {
        String traceId = getTraceId();
        String spanId = getSpanId();
        log.error("{} [traceId={}, spanId={}]", message, traceId, spanId, throwable);
    }
}
