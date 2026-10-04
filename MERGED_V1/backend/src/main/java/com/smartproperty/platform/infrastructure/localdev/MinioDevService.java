package com.smartproperty.platform.infrastructure.localdev;

import java.util.List;

final class MinioDevService {
    private final DevScriptRunner runner;

    MinioDevService(DevScriptRunner runner) {
        this.runner = runner;
    }

    void prepare(LocalRunContext context) {
        var result = runner.run(context.configuration(), "start-minio.ps1", List.of(
                "-ManagedByApplication",
                "-OwnerPid", Long.toString(ProcessHandle.current().pid()),
                "-OwnerToken", context.ownerToken(),
                "-RunId", context.runId(),
                "-MachineReadable"));
        if (result.exitCode() == 0) {
            if ("OWNED_BY_RUN".equals(result.value("MODE", ""))) {
                DevConsole.ok("MinIO API ready, OWNED_BY_RUN PID=" + result.value("PID", "-"));
            } else {
                DevConsole.reuse("MinIO API ready, EXTERNAL_REUSED PID=" + result.value("PID", "-"));
            }
        } else {
            DevConsole.warn("MinIO unavailable; file APIs will return 503");
        }
    }
}
