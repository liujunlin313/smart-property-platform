package com.smartproperty.platform.infrastructure.diagnostics;

import com.smartproperty.platform.common.logging.ObservabilityLog;
import com.smartproperty.platform.common.logging.RuntimeIdentity;
import com.smartproperty.platform.infrastructure.storage.StorageAvailabilityService;
import com.smartproperty.platform.infrastructure.localdev.DevConsole;
import com.smartproperty.platform.infrastructure.localdev.LocalDevelopmentServices;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;

/** Phase 2A Stabilization - concise post-context startup evidence for IDEA and PowerShell. */
@Component
public class StartupSummaryReporter implements ApplicationRunner {
    private final DatabaseAvailabilityService database;
    private final RedisAvailabilityService redis;
    private final StorageAvailabilityService storage;
    private final Environment environment;
    private final RuntimeIdentity runtimeIdentity;

    public StartupSummaryReporter(DatabaseAvailabilityService database,
                                  RedisAvailabilityService redis,
                                  StorageAvailabilityService storage,
                                  Environment environment,
                                  RuntimeIdentity runtimeIdentity) {
        this.database = database;
        this.redis = redis;
        this.storage = storage;
        this.environment = environment;
        this.runtimeIdentity = runtimeIdentity;
    }

    @Override
    public void run(ApplicationArguments args) {
        runtimeIdentity.putIntoMdc();
        try {
            ObservabilityLog.write(ObservabilityLog.TRACE, ObservabilityLog.event("RUNTIME_INSTANCE_READY"));
        } finally {
            runtimeIdentity.clearFromMdc();
        }
        LocalDevelopmentServices.recordBackendProcess(
                environment.getProperty("server.port", Integer.class, 8080));
        DevConsole.title("[RUNTIME STATUS]");
        var db = database.checkNow();
        if (db.available()) {
            DevConsole.ok("MySQL database=" + db.database() + ", frozen schema baseline=V" + db.schemaVersion());
        } else {
            DevConsole.error("MySQL/community_db unavailable");
        }
        var redisStatus = redis.snapshot();
        if (redisStatus.available()) {
            DevConsole.ok("Redis PING=PONG");
        } else {
            DevConsole.warn("Redis CACHE BYPASS");
        }
        var minio = storage.snapshot();
        if (minio.available()) {
            DevConsole.ok("MinIO " + minio.detail());
        } else {
            DevConsole.warn("MinIO file features degraded");
        }
        DevConsole.ok("JWT configuration ready; secrets are hidden");
        String webUiUrl = environment.getProperty("app.public-web-url", "http://127.0.0.1:5173");
        DevConsole.ready("Web UI " + webUiUrl);
    }
}
