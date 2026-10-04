package com.smartproperty.platform.infrastructure.localdev;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;

public final class DevConsole {
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static volatile Path startupLog;

    private DevConsole() {
    }

    static void initialize(LocalRunContext context) {
        startupLog = context.logDirectory().resolve("startup.log");
    }

    public static void banner(String runId) {
        print(CYAN, "[STARTUP] Smart Property local development run=" + runId);
    }

    public static void title(String message) {
        print(CYAN, message);
    }

    public static void ok(String message) {
        print(GREEN, "[OK] " + message);
    }

    public static void ready(String message) {
        print(GREEN, "[READY] " + message);
    }

    public static void reuse(String message) {
        print(YELLOW, "[REUSE] " + message);
    }

    public static void warn(String message) {
        print(YELLOW, "[WARN] " + message);
    }

    public static void error(String message) {
        print(RED, "[ERROR] " + message);
    }

    public static void stop(String message) {
        print(YELLOW, "[STOP] " + message);
    }

    private static synchronized void print(String color, String message) {
        System.out.println(color + message + RESET);
        if (startupLog == null) {
            return;
        }
        String line = OffsetDateTime.now() + " " + message + System.lineSeparator();
        try {
            Files.writeString(startupLog, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Startup diagnostics must never mask the actual application result.
        }
    }
}
