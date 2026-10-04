package com.smartproperty.platform.common.logging.flow;

import com.smartproperty.platform.common.logging.ObservabilityLog;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/** Captures only application controllers and services; framework internals are excluded. */
@Aspect
@Component
public class BusinessFlowAspect {
    @Around("execution(public * com.smartproperty.platform..controller..*(..))")
    public Object controller(ProceedingJoinPoint joinPoint) throws Throwable {
        FlowTrace.controller(signature(joinPoint));
        return joinPoint.proceed();
    }

    @Around("execution(public * com.smartproperty.platform..service..*(..))")
    public Object service(ProceedingJoinPoint joinPoint) throws Throwable {
        String signature = signature(joinPoint);
        FlowTrace.service(signature);
        long started = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            traceService(signature, (System.nanoTime() - started) / 1_000_000L, "SUCCESS", result);
            return result;
        } catch (Throwable exception) {
            traceService(signature, (System.nanoTime() - started) / 1_000_000L, "FAILURE", null);
            throw exception;
        }
    }

    private String signature(ProceedingJoinPoint joinPoint) {
        MethodSignature method = (MethodSignature) joinPoint.getSignature();
        return joinPoint.getTarget().getClass().getSimpleName() + "." + method.getName();
    }

    private void traceService(String signature, long durationMs, String result, Object value) {
        if (FlowTrace.current() == null) return;
        var fields = ObservabilityLog.event("SERVICE_CALL");
        fields.put("service", signature);
        fields.put("duration_ms", durationMs);
        fields.put("result", result);
        fields.put("result_summary", summary(value));
        ObservabilityLog.write(ObservabilityLog.TRACE, fields);
    }

    private String summary(Object value) {
        if (value == null) return "none";
        if (value instanceof java.util.Collection<?> rows) return "list_count=" + rows.size();
        if (value instanceof Number number) return "number=" + number;
        return "object";
    }
}
