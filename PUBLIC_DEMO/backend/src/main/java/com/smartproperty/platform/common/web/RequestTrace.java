package com.smartproperty.platform.common.web;

import org.slf4j.MDC;

public final class RequestTrace {
    public static final String MDC_KEY = "requestId";
    public static final String HEADER = "X-Request-ID";
    public static final String LEGACY_HEADER = "X-Trace-Id";
    public static final String LOAD_TEST_HEADER = "X-Load-Test-ID";
    public static final String LOAD_TEST_MDC_KEY = "loadTestId";

    private RequestTrace() {
    }

    public static String currentTraceId() {
        String traceId = MDC.get(MDC_KEY);
        return traceId == null || traceId.isBlank() ? "-" : traceId;
    }

    public static String currentLoadTestId() {
        String id = MDC.get(LOAD_TEST_MDC_KEY);
        return id == null || id.isBlank() ? "-" : id;
    }
}
