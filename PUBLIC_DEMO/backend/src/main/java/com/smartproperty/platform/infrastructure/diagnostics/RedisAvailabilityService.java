package com.smartproperty.platform.infrastructure.diagnostics;

import com.smartproperty.platform.infrastructure.localdev.DevConsole;

import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Phase 2A Stabilization - 2026-08-22
 * Redis is an optional performance layer. Every failure switches to CACHE BYPASS;
 * MySQL remains the only source of truth.
 */
@Component
public class RedisAvailabilityService {
    private static final long CHECK_CACHE_MILLIS = 4_000L;

    private final RedisConnectionFactory connectionFactory;
    private volatile boolean available;
    private volatile String detail = "not checked";
    private volatile long checkedAtMillis;

    public RedisAvailabilityService(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @PostConstruct
    void initialize() {
        checkNow();
    }

    public synchronized boolean checkNow() {
        boolean previous = available;
        try (RedisConnection connection = connectionFactory.getConnection()) {
            String pong = connection.ping();
            available = "PONG".equalsIgnoreCase(pong);
            detail = available ? "PING = PONG" : "unexpected ping response";
            if (available && !previous) {
                DevConsole.ok("Redis PING = PONG; cache infrastructure ready");
            }
        } catch (RuntimeException exception) {
            available = false;
            detail = safeReason(exception);
            if (previous || checkedAtMillis == 0L) {
                DevConsole.warn("Redis unavailable; CACHE BYPASS active: " + detail);
            }
        } finally {
            checkedAtMillis = System.currentTimeMillis();
        }
        return available;
    }

    public Snapshot snapshot() {
        if (System.currentTimeMillis() - checkedAtMillis > CHECK_CACHE_MILLIS) {
            checkNow();
        }
        return new Snapshot(available, available ? "READY" : "BYPASS", detail,
                Instant.ofEpochMilli(checkedAtMillis));
    }

    private String safeReason(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        String oneLine = message.replace('\r', ' ').replace('\n', ' ').trim();
        return exception.getClass().getSimpleName() + ": "
                + oneLine.substring(0, Math.min(oneLine.length(), 180));
    }

    public record Snapshot(boolean available, String mode, String detail, Instant checkedAt) {
    }
}
