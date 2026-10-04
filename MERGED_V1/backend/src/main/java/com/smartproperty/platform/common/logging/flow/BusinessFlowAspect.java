package com.smartproperty.platform.common.logging.flow;

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
        FlowTrace.service(signature(joinPoint));
        return joinPoint.proceed();
    }

    private String signature(ProceedingJoinPoint joinPoint) {
        MethodSignature method = (MethodSignature) joinPoint.getSignature();
        return joinPoint.getTarget().getClass().getSimpleName() + "." + method.getName();
    }
}
