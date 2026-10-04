package com.smartproperty.platform.infrastructure.localdev;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Single idempotent entry point used by IDEA and PowerShell backend startup. */
public final class LocalDevelopmentServices {
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final AtomicReference<LocalRunContext> CURRENT = new AtomicReference<>();

    private LocalDevelopmentServices() {
    }

    public static void start() {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }
        var discovered = LocalDevelopmentConfiguration.discover();
        if (discovered.isEmpty()
                || !discovered.get().booleanValue("APP_DEV_BOOTSTRAP", true)) {
            return;
        }

        LocalRunContext context = LocalRunContext.create(discovered.get());
        CURRENT.set(context);
        DevConsole.initialize(context);
        DevConsole.banner(context.runId());
        DevConsole.ok("Local configuration loaded; secret values are hidden");
        DevShutdownManager shutdown = new DevShutdownManager(context);
        try {
            new BackendPortCheck().verify(context.configuration());
            DevScriptRunner runner = new DevScriptRunner();
            new MysqlDevCheck(runner).verify(context);
            new RedisDevService(runner).prepare(context);
            new MinioDevService(runner).prepare(context);
            shutdown.register();
            new DevProcessRegistry().armOwnerWatcher(context);
            DevConsole.title("[APPLICATION] Starting Spring Boot");
        } catch (RuntimeException exception) {
            shutdown.close();
            DevConsole.error(exception.getMessage());
            throw exception;
        }
    }

    public static void recordBackendProcess(int port) {
        LocalRunContext context = CURRENT.get();
        if (context != null) {
            new DevProcessRegistry().recordApplication(context, port);
        }
    }
}
