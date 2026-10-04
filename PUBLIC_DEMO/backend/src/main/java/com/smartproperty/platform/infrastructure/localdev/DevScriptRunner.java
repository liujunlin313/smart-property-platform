package com.smartproperty.platform.infrastructure.localdev;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

final class DevScriptRunner {
    ScriptResult run(LocalDevelopmentConfiguration configuration,
                     String scriptName,
                     List<String> additionalArguments) {
        Path script = configuration.projectDirectory().resolve("deploy/scripts/dev").resolve(scriptName);
        if (!Files.isRegularFile(script)) {
            throw new IllegalStateException("Development script is missing: " + script);
        }
        List<String> command = new ArrayList<>(List.of(
                "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                "-File", script.toString(), "-ConfigFile", configuration.configFile().toString()));
        command.addAll(additionalArguments);
        Path outputFile = configuration.projectDirectory().resolve("deploy/runtime/dev-processes")
                .resolve("script-" + UUID.randomUUID() + ".out.log");
        try {
            Files.createDirectories(outputFile.getParent());
            ProcessBuilder builder = new ProcessBuilder(command)
                    .directory(configuration.projectDirectory().toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(outputFile.toFile());
            Process process = builder.start();
            if (!process.waitFor(45, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("Development script timed out: " + scriptName);
            }
            List<String> output = Files.exists(outputFile)
                    ? Files.readAllLines(outputFile)
                    : List.of();
            return new ScriptResult(process.exitValue(), output, parseValues(output));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot start PowerShell script: " + scriptName, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for " + scriptName, exception);
        } finally {
            try {
                Files.deleteIfExists(outputFile);
            } catch (IOException ignored) {
                // A diagnostic temp file must never mask the actual startup result.
                if (Files.exists(outputFile)) {
                    outputFile.toFile().deleteOnExit();
                }
            }
        }
    }

    private Map<String, String> parseValues(List<String> output) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String line : output) {
            int separator = line.indexOf('=');
            if (separator > 0 && line.substring(0, separator).matches("[A-Z_]+")) {
                values.put(line.substring(0, separator), line.substring(separator + 1));
            }
        }
        return values;
    }

    record ScriptResult(int exitCode, List<String> output, Map<String, String> values) {
        String value(String key, String fallback) {
            return values.getOrDefault(key, fallback);
        }

        String lastOutput() {
            return output.isEmpty() ? "no script output" : output.get(output.size() - 1);
        }
    }
}
