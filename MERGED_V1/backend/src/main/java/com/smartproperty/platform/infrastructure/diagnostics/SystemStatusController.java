package com.smartproperty.platform.infrastructure.diagnostics;

import com.smartproperty.platform.infrastructure.storage.StorageAvailabilityService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** Phase 2A Stabilization - the one endpoint polled by the browser heartbeat. */
@RestController
@RequestMapping("/api/v1/system")
public class SystemStatusController {
    private final DatabaseAvailabilityService database;
    private final RedisAvailabilityService redis;
    private final StorageAvailabilityService storage;

    public SystemStatusController(DatabaseAvailabilityService database,
                                  RedisAvailabilityService redis,
                                  StorageAvailabilityService storage) {
        this.database = database;
        this.redis = redis;
        this.storage = storage;
    }

    @GetMapping("/heartbeat")
    public HeartbeatResponse heartbeat(
            @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        var databaseStatus = database.snapshot();
        var redisStatus = redis.snapshot();
        var storageStatus = storage.snapshot();
        String overall = databaseStatus.available()
                ? (redisStatus.available() && storageStatus.available() ? "UP" : "DEGRADED")
                : "DOWN";
        return new HeartbeatResponse(
                overall,
                Instant.now(),
                jwt != null,
                jwt == null ? null : jwt.getExpiresAt(),
                new DependencyStatus(databaseStatus.available(), databaseStatus.database(),
                        "schema V" + databaseStatus.schemaVersion()),
                new DependencyStatus(redisStatus.available(), "Redis", redisStatus.mode()),
                new DependencyStatus(storageStatus.available(), "MinIO", storageStatus.detail()),
                redisStatus.available() ? "READY" : "BYPASS"
        );
    }

    public record DependencyStatus(boolean available, String name, String detail) {
    }

    public record HeartbeatResponse(
            String status,
            Instant serverTime,
            boolean authenticated,
            Instant accessTokenExpiresAt,
            DependencyStatus mysql,
            DependencyStatus redis,
            DependencyStatus minio,
            String cacheMode
    ) {
    }
}
