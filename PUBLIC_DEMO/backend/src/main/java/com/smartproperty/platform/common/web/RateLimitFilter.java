package com.smartproperty.platform.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartproperty.platform.common.api.ApiResponse;
import com.smartproperty.platform.common.logging.SecurityEventLog;
import com.smartproperty.platform.infrastructure.security.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A deliberately small local guard for the localhost demonstration runtime.
 * It is a throttling boundary, never an authentication or authorization bypass.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class RateLimitFilter extends OncePerRequestFilter {
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = Clock.systemUTC();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled() || "OPTIONS".equalsIgnoreCase(request.getMethod()) || "/actuator/health".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Category category = categorize(request);
        String key = request.getRemoteAddr() + ':' + category.name();
        if (!allow(key, category.limit())) {
            SecurityEventLog.write(request, "RATE_LIMITED", "REJECTED", null, category.name());
            writeLimited(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean allow(String key, int limit) {
        long now = clock.millis();
        long windowMillis = Math.max(1, properties.windowSeconds()) * 1000L;
        Window window = windows.computeIfAbsent(key, ignored -> new Window(now));
        synchronized (window) {
            if (now - window.startedAt >= windowMillis) {
                window.startedAt = now;
                window.count = 0;
            }
            if (window.count >= Math.max(1, limit)) return false;
            window.count++;
            return true;
        }
    }

    private Category categorize(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method)
                && ("/api/v1/auth/login".equals(path) || "/api/v1/auth/refresh".equals(path))) {
            return new Category("AUTH", properties.authPerWindow());
        }
        if (request.getContentType() != null && request.getContentType().toLowerCase().startsWith("multipart/")) {
            return new Category("UPLOAD", properties.uploadPerWindow());
        }
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            if (path.contains("statistics") || path.contains("dashboard") || path.contains("search") || path.contains("map")) {
                return new Category("EXPENSIVE_READ", properties.expensiveReadPerWindow());
            }
            return new Category("READ", properties.readPerWindow());
        }
        return new Category("WRITE", properties.writePerWindow());
    }

    private void writeLimited(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", Integer.toString(Math.max(1, properties.windowSeconds())));
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(
                "RATE_LIMITED", "请求过于频繁，请稍后再试", null,
                429, RequestTrace.currentTraceId()));
    }

    private record Category(String name, int limit) { }

    private static final class Window {
        private long startedAt;
        private int count;
        private Window(long startedAt) { this.startedAt = startedAt; }
    }
}
