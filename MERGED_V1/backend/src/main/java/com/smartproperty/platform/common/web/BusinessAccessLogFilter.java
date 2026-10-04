package com.smartproperty.platform.common.web;

import com.smartproperty.platform.common.logging.HttpAccessLog;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/** Logs one concise behavior line for every meaningful backend API interaction. */
public final class BusinessAccessLogFilter extends OncePerRequestFilter {
    private static final Set<String> EXCLUDED = Set.of(
            "/api/v1/system/heartbeat", "/actuator/health", "/favicon.ico", "/favicon.svg");
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static final String RESET = "\u001B[0m";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long started = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (isBusinessRequest(request)) {
                long durationMs = (System.nanoTime() - started) / 1_000_000L;
                int status = response.getStatus();
                String consoleLine = "user=" + resolveActor(request) + " " + request.getMethod() + " "
                        + request.getRequestURI() + (request.getQueryString() == null ? "" : "?filtered")
                        + " status=" + status + " duration=" + durationMs + "ms";
                write(status, consoleLine, consoleLine + " trace=" + RequestTrace.currentTraceId());
            }
        }
    }

    private boolean isBusinessRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/") && !EXCLUDED.contains(path);
    }

    private String resolveActor(HttpServletRequest request) {
        String actor = RequestAuditContext.actor(request);
        if (actor != null) return actor;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) return "anonymous";
        Object username = authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt
                ? jwt.getClaim("username") : null;
        return username == null ? authentication.getName() : username.toString();
    }

    private void write(int status, String consoleLine, String fileLine) {
        String message = (status >= 500 ? RED : status >= 400 ? YELLOW : GREEN)
                + "[HTTP " + status + "] " + consoleLine + RESET;
        if (status >= 500) HttpAccessLog.CONSOLE.error(message);
        else if (status >= 400) HttpAccessLog.CONSOLE.warn(message);
        else HttpAccessLog.CONSOLE.info(message);
        HttpAccessLog.FILE.info(fileLine);
    }
}
