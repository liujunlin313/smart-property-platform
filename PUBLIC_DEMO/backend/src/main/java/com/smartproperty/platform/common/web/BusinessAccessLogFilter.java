package com.smartproperty.platform.common.web;

import com.smartproperty.platform.common.logging.HttpAccessLog;
import com.smartproperty.platform.common.logging.ObservabilityLog;
import com.smartproperty.platform.common.logging.SecurityEventLog;
import com.smartproperty.platform.common.logging.flow.FlowTrace;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** One safe HTTP JSON line and one generic write audit event per meaningful API request. */
public final class BusinessAccessLogFilter extends OncePerRequestFilter {
    private static final Set<String> EXCLUDED = Set.of("/api/v1/system/heartbeat", "/actuator/health", "/favicon.ico", "/favicon.svg");
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long started = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (isBusinessRequest(request)) logCompleted(request, response, (System.nanoTime() - started) / 1_000_000L);
        }
    }

    private void logCompleted(HttpServletRequest request, HttpServletResponse response, long durationMs) {
        int status = response.getStatus();
        Actor actor = resolveActor(request);
        FlowTrace.FlowRecord flow = FlowTrace.current();
        Map<String, Object> http = ObservabilityLog.event("HTTP_REQUEST");
        http.put("method", request.getMethod());
        http.put("path", request.getRequestURI());
        http.put("query_keys", request.getParameterMap().keySet().stream().filter(this::safeParameter).sorted().toList());
        http.put("authenticated", actor.authenticated());
        http.put("user_id", actor.accountId());
        http.put("username", actor.username());
        http.put("roles", actor.roles());
        http.put("remote_addr", request.getRemoteAddr());
        http.put("x_forwarded_for_raw", request.getHeader("X-Forwarded-For"));
        http.put("cf_connecting_ip_raw", request.getHeader("CF-Connecting-IP"));
        http.put("effective_client_ip", request.getRemoteAddr());
        http.put("client_ip_trusted", false);
        http.put("controller", flow == null ? null : flow.controller());
        http.put("status", status);
        http.put("duration_ms", durationMs);
        http.put("response_size", response.getHeader("Content-Length"));
        http.put("result", status < 400 ? "SUCCESS" : "FAILURE");
        ObservabilityLog.write(ObservabilityLog.HTTP, http);

        String concise = "user=" + (actor.username() == null ? "anonymous" : actor.username()) + " "
                + request.getMethod() + " " + request.getRequestURI() + " status=" + status + " duration=" + durationMs + "ms";
        if (status >= 500) HttpAccessLog.CONSOLE.error("[HTTP {}] {}", status, concise);
        else if (status >= 400) HttpAccessLog.CONSOLE.warn("[HTTP {}] {}", status, concise);
        else HttpAccessLog.CONSOLE.info("[HTTP {}] {}", status, concise);

        if (WRITE_METHODS.contains(request.getMethod()) && !request.getRequestURI().equals("/api/v1/dev/client-events")) {
            Map<String, Object> audit = ObservabilityLog.event("BUSINESS_WRITE");
            audit.put("action", request.getMethod() + " " + request.getRequestURI());
            audit.put("target_type", targetType(request.getRequestURI()));
            audit.put("target_id", targetId(request.getRequestURI()));
            audit.put("user_id", actor.accountId());
            audit.put("username", actor.username());
            audit.put("roles", actor.roles());
            audit.put("source_ip", request.getRemoteAddr());
            audit.put("result", status < 400 ? "SUCCESS" : "FAILURE");
            audit.put("status", status);
            ObservabilityLog.write(ObservabilityLog.AUDIT, audit);
        }
        if (status == 401 || status == 403) {
            SecurityEventLog.write(request, status == 401 ? "UNAUTHENTICATED_REQUEST" : "ACCESS_DENIED",
                    "FAILURE", actor.username(), status == 403 ? "ACCESS_DENIED" : "AUTH_REQUIRED");
        }
    }

    private boolean isBusinessRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/") && !EXCLUDED.contains(request.getRequestURI());
    }

    private boolean safeParameter(String key) {
        String value = key.toLowerCase();
        return !(value.contains("password") || value.contains("token") || value.contains("secret")
                || value.contains("authorization") || value.contains("phone") || value.contains("name") || value.contains("idcard"));
    }

    private Actor resolveActor(HttpServletRequest request) {
        String requestActor = RequestAuditContext.actor(request);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return new Actor(null, requestActor, java.util.List.of(), false);
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return new Actor(jwt.getSubject(), jwt.getClaimAsString("username"), jwt.getClaimAsStringList("roles"), true);
        }
        return new Actor(null, authentication.getName(), java.util.List.of(), true);
    }

    private String targetType(String path) {
        String[] parts = path.split("/");
        return parts.length > 3 ? parts[3] : "api";
    }

    private Long targetId(String path) {
        String[] parts = path.split("/");
        for (int index = parts.length - 1; index >= 0; index--) {
            try { return Long.valueOf(parts[index]); } catch (NumberFormatException ignored) { }
        }
        return null;
    }

    private record Actor(String accountId, String username, java.util.List<String> roles, boolean authenticated) { }
}
