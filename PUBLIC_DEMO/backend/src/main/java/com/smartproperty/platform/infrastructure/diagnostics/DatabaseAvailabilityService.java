package com.smartproperty.platform.infrastructure.diagnostics;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Phase 2A Stabilization - cached heartbeat check for the single community_db. */
@Component
public class DatabaseAvailabilityService {
    private static final long CHECK_CACHE_MILLIS = 4_000L;

    private final JdbcTemplate jdbcTemplate;
    private volatile boolean available;
    private volatile String databaseName = "unknown";
    private volatile String schemaVersion = "unknown";
    private volatile long checkedAtMillis;

    public DatabaseAvailabilityService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public synchronized Snapshot checkNow() {
        try {
            databaseName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            schemaVersion = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(MAX(version), 'NONE') FROM flyway_schema_history", String.class);
            available = true;
        } catch (RuntimeException exception) {
            available = false;
        } finally {
            checkedAtMillis = System.currentTimeMillis();
        }
        return snapshotWithoutRefresh();
    }

    public Snapshot snapshot() {
        if (System.currentTimeMillis() - checkedAtMillis > CHECK_CACHE_MILLIS) {
            return checkNow();
        }
        return snapshotWithoutRefresh();
    }

    private Snapshot snapshotWithoutRefresh() {
        return new Snapshot(available, databaseName, schemaVersion, Instant.ofEpochMilli(checkedAtMillis));
    }

    public record Snapshot(boolean available, String database, String schemaVersion, Instant checkedAt) {
    }
}
