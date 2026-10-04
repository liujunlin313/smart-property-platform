package com.smartproperty.platform.common.logging.flow;

import com.smartproperty.platform.common.logging.HttpAccessLog;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashSet;

/** Emits one compact controller → service → mapper → SQL tree per API request. */
public final class CompactFlowLogFilter extends OncePerRequestFilter {
    private final FlowLoggingProperties properties;

    public CompactFlowLogFilter(FlowLoggingProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.flowEnabled() || !request.getRequestURI().startsWith("/api/")
                || request.getRequestURI().equals("/api/v1/system/heartbeat");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        FlowTrace.begin(request.getMethod(), request.getRequestURI() + safeQuery(request));
        try {
            filterChain.doFilter(request, response);
        } finally {
            FlowTrace.FlowRecord record = FlowTrace.complete();
            if (record != null) write(record, response.getStatus(), resolveUsername());
        }
    }

    private void write(FlowTrace.FlowRecord record, int status, String username) {
        StringBuilder line = new StringBuilder("[FLOW] ").append(record.method()).append(' ')
                .append(record.uri()).append(" user=").append(username);
        if (record.controller() != null) line.append("\n  ├─ Controller  ").append(record.controller());
        for (String service : new LinkedHashSet<>(record.services())) line.append("\n  ├─ Service     ").append(service);
        int index = 0;
        for (FlowTrace.MapperCall mapper : record.mappers()) {
            line.append("\n  ├─ Mapper      ").append(mapper.statement());
            line.append("\n  ├─ SQL[").append(++index).append("]     ").append(mapper.sql())
                    .append("  (").append(mapper.durationMs()).append("ms, ").append(mapper.outcome()).append(")");
        }
        line.append("\n  └─ ").append(status).append(" ").append(status < 400 ? "OK" : "ERROR")
                .append("      ").append(record.durationMs()).append("ms");
        if (status >= 500) HttpAccessLog.CONSOLE.error(line.toString());
        else if (status >= 400) HttpAccessLog.CONSOLE.warn(line.toString());
        else HttpAccessLog.CONSOLE.info(line.toString());
        // Mirror the same compact, already-sanitized tree to the existing access file sink.
        HttpAccessLog.FILE.info(line.toString());
    }

    private String resolveUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) return "anonymous";
        if (authentication.getPrincipal() instanceof Jwt jwt && jwt.getClaimAsString("username") != null) {
            return jwt.getClaimAsString("username");
        }
        return authentication.getName();
    }

    private String safeQuery(HttpServletRequest request) {
        if (request.getQueryString() == null || request.getQueryString().isBlank()) return "";
        return "?" + request.getParameterMap().keySet().stream()
                .filter(this::safeParameter).map(key -> key + "=" + request.getParameter(key)).reduce((a, b) -> a + "&" + b).orElse("");
    }

    private boolean safeParameter(String key) {
        String normalized = key.toLowerCase();
        return !(normalized.contains("password") || normalized.contains("token") || normalized.contains("secret")
                || normalized.contains("authorization") || normalized.contains("phone") || normalized.contains("name"));
    }
}
