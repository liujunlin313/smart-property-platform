package com.smartproperty.platform.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.smartproperty.platform.common.logging.RuntimeIdentity;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HexFormat;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RequestTraceFilter extends OncePerRequestFilter {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final RuntimeIdentity runtimeIdentity;

    public RequestTraceFilter(RuntimeIdentity runtimeIdentity) {
        this.runtimeIdentity = runtimeIdentity;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = incomingOrNewTraceId(request.getHeader(RequestTrace.HEADER));
        if (traceId == null) traceId = incomingOrNewTraceId(request.getHeader(RequestTrace.LEGACY_HEADER));
        if (traceId == null) traceId = newTraceId();
        MDC.put(RequestTrace.MDC_KEY, traceId);
        String loadTestId = request.getHeader(RequestTrace.LOAD_TEST_HEADER);
        if (loadTestId != null && loadTestId.matches("[A-Za-z0-9_-]{6,80}")) {
            MDC.put(RequestTrace.LOAD_TEST_MDC_KEY, loadTestId);
        }
        runtimeIdentity.putIntoMdc();
        response.setHeader(RequestTrace.HEADER, traceId);
        response.setHeader(RequestTrace.LEGACY_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(RequestTrace.MDC_KEY);
            MDC.remove(RequestTrace.LOAD_TEST_MDC_KEY);
            runtimeIdentity.clearFromMdc();
        }
    }

    private String incomingOrNewTraceId(String candidate) {
        if (candidate != null && candidate.matches("[A-Za-z0-9_-]{6,40}")) {
            return candidate;
        }
        return null;
    }

    private String newTraceId() {
        byte[] bytes = new byte[6];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().withUpperCase().formatHex(bytes);
    }
}
