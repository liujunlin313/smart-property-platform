package com.smartproperty.platform.infrastructure.localdev;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class DevShutdownManager implements AutoCloseable {
    private final LocalRunContext context;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    DevShutdownManager(LocalRunContext context) {
        this.context = context;
    }

    void register() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close,
                "smart-property-dev-cleanup-" + context.runId()));
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        DevConsole.stop("Application is stopping; reclaiming only RUN_ID=" + context.runId());
        var script = context.configuration().projectDirectory()
                .resolve("deploy/scripts/dev/stop-managed-dependencies.ps1");
        ProcessBuilder builder = new ProcessBuilder(
                "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                "-File", script.toString(),
                "-RunId", context.runId(),
                "-OwnerToken", context.ownerToken())
                .directory(context.configuration().projectDirectory().toFile());
        try {
            Process process = builder.start();
            if (!process.waitFor(15, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                DevConsole.warn("Dependency cleanup exceeded 15 seconds");
            }
        } catch (IOException exception) {
            DevConsole.warn("Cannot start dependency cleanup: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            DevConsole.warn("Dependency cleanup was interrupted");
        }
    }
}
