package com.smartproperty.platform.infrastructure.localdev;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;

final class DevProcessRegistry {
    void recordApplication(LocalRunContext context, int port) {
        ProcessHandle current = ProcessHandle.current();
        String executable = current.info().command().orElse("");
        Instant started = current.info().startInstant().orElse(Instant.now());
        long dotNetTicks = (started.toEpochMilli() + 62_135_596_800_000L) * 10_000L;
        var runDirectory = context.configuration().projectDirectory()
                .resolve("deploy/runtime/dev-processes/runs").resolve(context.runId());
        String json = "{\n"
                + "  \"service\": \"backend\",\n"
                + "  \"processId\": " + current.pid() + ",\n"
                + "  \"executable\": \"" + jsonEscape(executable) + "\",\n"
                + "  \"processStartTimeUtcTicks\": " + dotNetTicks + ",\n"
                + "  \"ownerPid\": " + current.pid() + ",\n"
                + "  \"ownerToken\": \"" + jsonEscape(context.ownerToken()) + "\",\n"
                + "  \"runId\": \"" + jsonEscape(context.runId()) + "\",\n"
                + "  \"ports\": [" + port + "]\n"
                + "}\n";
        try {
            Files.createDirectories(runDirectory);
            Files.writeString(runDirectory.resolve("backend.json"), json, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            DevConsole.warn("Cannot write backend process record: " + exception.getMessage());
        }
    }

    void armOwnerWatcher(LocalRunContext context) {
        var script = context.configuration().projectDirectory()
                .resolve("deploy/scripts/dev/watch-owner.ps1");
        ProcessBuilder builder = new ProcessBuilder(
                "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                "-File", script.toString(),
                "-OwnerPid", Long.toString(ProcessHandle.current().pid()),
                "-OwnerToken", context.ownerToken(),
                "-RunId", context.runId())
                .directory(context.configuration().projectDirectory().toFile());
        try {
            builder.redirectOutput(context.logDirectory().resolve("owner-watcher.log").toFile());
            builder.redirectErrorStream(true);
            builder.start();
            DevConsole.ok("Exact PID owner watcher armed");
        } catch (IOException exception) {
            DevConsole.warn("Cannot arm owner watcher: " + exception.getMessage());
        }
    }

    private String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
