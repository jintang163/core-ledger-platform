package com.bank.core.account.config;

import brave.Span;
import brave.Tracer;
import brave.propagation.TraceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.Tag;
import org.apache.skywalking.apm.toolkit.trace.Tags;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TracingConfig {

    private final Tracer tracer;

    private static final String TRACE_ID_KEY = "traceId";
    private static final String SPAN_ID_KEY = "spanId";
    private static final String TRANSACTION_ID_KEY = "transactionId";
    private static final String GLOBAL_TX_ID_KEY = "globalTxId";

    @Bean
    public WebMvcConfigurer tracingWebMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new TracingInterceptor())
                        .addPathPatterns("/**")
                        .excludePathPatterns("/actuator/**", "/swagger/**", "/doc.html");
            }
        };
    }

    @Bean
    public SeataTracingAspect seataTracingAspect() {
        return new SeataTracingAspect(tracer);
    }

    public static class TracingInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            String traceId = request.getHeader("sw8");
            if (traceId != null) {
                MDC.put(TRACE_ID_KEY, traceId);
            }
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                    Object handler, Exception ex) {
            MDC.remove(TRACE_ID_KEY);
            MDC.remove(SPAN_ID_KEY);
        }
    }

    @Slf4j
    @Aspect
    @RequiredArgsConstructor
    public static class SeataTracingAspect {

        private final Tracer tracer;

        @Pointcut("@annotation(io.seata.spring.annotation.GlobalTransactional)")
        public void globalTransactionalPointcut() {}

        @Pointcut("execution(* com.bank.core.account.service..*.try*(..)) " +
                "|| execution(* com.bank.core.account.service..*.confirm*(..)) " +
                "|| execution(* com.bank.core.account.service..*.cancel*(..))")
        public void tccPhasePointcut() {}

        @Around("globalTransactionalPointcut()")
        @Trace(operationName = "seata-global-transaction")
        @Tags({
                @Tag(key = "globalTxId", value = "returnedObj.xid"),
                @Tag(key = "transactionName", value = "arg[0].businessNo")
        })
        public Object aroundGlobalTransactional(ProceedingJoinPoint joinPoint) throws Throwable {
            String methodName = joinPoint.getSignature().getName();
            Object[] args = joinPoint.getArgs();

            Span span = tracer.nextSpan().name("seata-global-tx:" + methodName).start();
            try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
                span.tag("phase", "global");
                span.tag("component", "seata");
                span.tag("transaction.type", "TCC/Saga");

                if (args.length > 0 && args[0] != null) {
                    span.tag("businessNo", extractBusinessNo(args[0]));
                }

                long startTime = System.currentTimeMillis();
                try {
                    Object result = joinPoint.proceed();
                    span.tag("status", "success");
                    span.tag("duration", String.valueOf(System.currentTimeMillis() - startTime));
                    log.info("全局事务执行成功, traceId={}, duration={}ms",
                            span.context().traceIdString(), System.currentTimeMillis() - startTime);
                    return result;
                } catch (Exception e) {
                    span.tag("status", "failed");
                    span.tag("error", e.getMessage());
                    span.error(e);
                    log.error("全局事务执行失败, traceId={}, error={}",
                            span.context().traceIdString(), e.getMessage(), e);
                    throw e;
                }
            } finally {
                span.finish();
            }
        }

        @Around("tccPhasePointcut()")
        @Trace(operationName = "seata-tcc-phase")
        public Object aroundTccPhase(ProceedingJoinPoint joinPoint) throws Throwable {
            String methodName = joinPoint.getSignature().getName();
            String phase = determineTccPhase(methodName);

            Span span = tracer.nextSpan().name("seata-tcc:" + phase).start();
            try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
                span.tag("phase", phase);
                span.tag("component", "seata");
                span.tag("transaction.phase", phase);

                long startTime = System.currentTimeMillis();
                try {
                    Object result = joinPoint.proceed();
                    span.tag("status", "success");
                    span.tag("duration", String.valueOf(System.currentTimeMillis() - startTime));
                    log.debug("TCC {} 阶段执行成功, traceId={}, duration={}ms",
                            phase, span.context().traceIdString(), System.currentTimeMillis() - startTime);
                    return result;
                } catch (Exception e) {
                    span.tag("status", "failed");
                    span.tag("error", e.getMessage());
                    span.error(e);
                    log.error("TCC {} 阶段执行失败, traceId={}, error={}",
                            phase, span.context().traceIdString(), e.getMessage(), e);
                    throw e;
                }
            } finally {
                span.finish();
            }
        }

        private String determineTccPhase(String methodName) {
            if (methodName.startsWith("try")) {
                return "TRY";
            } else if (methodName.startsWith("confirm")) {
                return "CONFIRM";
            } else if (methodName.startsWith("cancel")) {
                return "CANCEL";
            }
            return "UNKNOWN";
        }

        private String extractBusinessNo(Object arg) {
            try {
                java.lang.reflect.Method method = arg.getClass().getMethod("getBusinessNo");
                Object result = method.invoke(arg);
                return result != null ? result.toString() : "unknown";
            } catch (Exception e) {
                return "unknown";
            }
        }
    }

    public static Map<String, String> getTracingContext() {
        Map<String, String> context = new HashMap<>();
        TraceContext traceContext = brave.propagation.ThreadLocalSpan.current().context();
        if (traceContext != null) {
            context.put(TRACE_ID_KEY, traceContext.traceIdString());
            context.put(SPAN_ID_KEY, traceContext.spanIdString());
        }
        return context;
    }

    public static String getCurrentTraceId() {
        TraceContext traceContext = brave.propagation.ThreadLocalSpan.current().context();
        return traceContext != null ? traceContext.traceIdString() : null;
    }

    public static void setTransactionTag(String transactionId, String globalTxId) {
        Span span = tracer.currentSpan();
        if (span != null) {
            span.tag(TRANSACTION_ID_KEY, transactionId);
            span.tag(GLOBAL_TX_ID_KEY, globalTxId);
        }
        MDC.put(TRANSACTION_ID_KEY, transactionId);
        if (globalTxId != null) {
            MDC.put(GLOBAL_TX_ID_KEY, globalTxId);
        }
    }

    public static void clearTransactionTag() {
        MDC.remove(TRANSACTION_ID_KEY);
        MDC.remove(GLOBAL_TX_ID_KEY);
    }
}
