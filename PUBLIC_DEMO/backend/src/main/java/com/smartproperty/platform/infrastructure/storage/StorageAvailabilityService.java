package com.smartproperty.platform.infrastructure.storage;

import com.smartproperty.platform.infrastructure.localdev.DevConsole;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Phase 2A Stabilization - 2026-08-22
 * MinIO is recoverable infrastructure: failure disables only file operations and
 * never hides database-backed asset functions.
 */
@Component
public class StorageAvailabilityService {
    private static final long CHECK_CACHE_MILLIS = 4_000L;

    private final MinioClient client;
    private final StorageProperties properties;
    private volatile boolean available;
    private volatile String detail = "not checked";
    private volatile long checkedAtMillis;

    public StorageAvailabilityService(MinioClient client, StorageProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @PostConstruct
    void initialize() {
        checkNow();
    }

    public boolean ensureReady() {
        if (System.currentTimeMillis() - checkedAtMillis > CHECK_CACHE_MILLIS) {
            checkNow();
        }
        return available;
    }

    public synchronized boolean checkNow() {
        boolean previous = available;
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder()
                    .bucket(properties.propertyBucket()).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(properties.propertyBucket()).build());
            }
            available = true;
            detail = "bucket " + properties.propertyBucket() + " ready";
            if (!previous) {
                DevConsole.ok("MinIO " + detail);
            }
        } catch (Exception exception) {
            available = false;
            detail = safeReason(exception);
            if (previous || checkedAtMillis == 0L) {
                DevConsole.warn("MinIO unavailable; file APIs are degraded: " + detail);
            }
        } finally {
            checkedAtMillis = System.currentTimeMillis();
        }
        return available;
    }

    public void markUnavailable(Exception exception) {
        boolean previous = available;
        available = false;
        detail = safeReason(exception);
        checkedAtMillis = System.currentTimeMillis();
        if (previous) {
            DevConsole.warn("MinIO connection lost; file APIs are degraded: " + detail);
        }
    }

    public Snapshot snapshot() {
        ensureReady();
        return new Snapshot(available, detail, Instant.ofEpochMilli(checkedAtMillis));
    }

    private String safeReason(Exception exception) {
        String name = exception.getClass().getSimpleName();
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return name;
        }
        String oneLine = message.replace('\r', ' ').replace('\n', ' ').trim();
        return name + ": " + oneLine.substring(0, Math.min(oneLine.length(), 180));
    }

    public record Snapshot(boolean available, String detail, Instant checkedAt) {
    }
}
