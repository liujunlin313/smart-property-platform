package com.smartproperty.platform.infrastructure.localdev;

import java.util.List;

final class RedisDevService {
    private final DevScriptRunner runner;

    RedisDevService(DevScriptRunner runner) {
        this.runner = runner;
    }

    void prepare(LocalRunContext context) {
        var result = runner.run(context.configuration(), "start-redis.ps1", managedArguments(context));
        if (result.exitCode() == 0) {
            report("Redis", result);
        } else {
            DevConsole.warn("Redis unavailable; cache mode is BYPASS");
        }
    }

    private void report(String service, DevScriptRunner.ScriptResult result) {
        if ("OWNED_BY_RUN".equals(result.value("MODE", ""))) {
            DevConsole.ok(service + " PING=PONG, OWNED_BY_RUN PID=" + result.value("PID", "-"));
        } else {
            DevConsole.reuse(service + " PING=PONG, EXTERNAL_REUSED PID=" + result.value("PID", "-"));
        }
    }

    private List<String> managedArguments(LocalRunContext context) {
        return List.of("-ManagedByApplication",
                "-OwnerPid", Long.toString(ProcessHandle.current().pid()),
                "-OwnerToken", context.ownerToken(),
                "-RunId", context.runId(),
                "-MachineReadable");
    }
}
