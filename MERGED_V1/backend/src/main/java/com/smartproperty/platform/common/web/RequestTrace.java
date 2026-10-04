package com.smartproperty.platform.common.web;

import org.slf4j.MDC;

public final class RequestTrace {
    public static final String MDC_KEY = "traceId";
    public static final String HEADER = "X-Trace-Id";

    private RequestTrace() {
    }

    public static String currentTraceId() {
        String traceId = MDC.get(MDC_KEY);
        return traceId == null || traceId.isBlank() ? "-" : traceId;
    }
}
