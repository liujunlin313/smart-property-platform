package com.smartproperty.platform.infrastructure.localdev;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

record LocalRunContext(
        LocalDevelopmentConfiguration configuration,
        String runId,
        String ownerToken,
        Path logDirectory
) {
    private static final DateTimeFormatter RUN_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    static LocalRunContext create(LocalDevelopmentConfiguration configuration) {
        String configured = firstNonBlank(System.getProperty("APP_RUN_ID"), System.getenv("APP_RUN_ID"));
        String runId = configured == null
                ? RUN_FORMAT.format(LocalDateTime.now()) + "-p" + ProcessHandle.current().pid()
                : configured;
        if (!runId.matches("[A-Za-z0-9._-]{6,80}")) {
            throw new IllegalStateException("Invalid APP_RUN_ID: " + runId);
        }
        Path logDirectory = configuration.projectDirectory().resolve("logs").resolve("run-" + runId);
        try {
            Files.createDirectories(logDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot create run log directory: " + logDirectory, exception);
        }
        String ownerToken = firstNonBlank(System.getProperty("APP_ORCHESTRATOR_OWNER_TOKEN"),
                System.getenv("APP_ORCHESTRATOR_OWNER_TOKEN"));
        if (ownerToken == null) {
            ownerToken = UUID.randomUUID().toString();
        }
        System.setProperty("APP_RUN_ID", runId);
        System.setProperty("APP_RUN_LOG_DIR", logDirectory.toString());
        return new LocalRunContext(configuration, runId, ownerToken, logDirectory);
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null || second.isBlank() ? null : second;
    }
}
