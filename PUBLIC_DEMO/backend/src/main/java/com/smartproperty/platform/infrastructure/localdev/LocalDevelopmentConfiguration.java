package com.smartproperty.platform.infrastructure.localdev;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Phase 2A Stabilization - 2026-08-22
 * Loads the one ignored local configuration file without printing any secret value.
 * A deliberately small KEY=VALUE parser keeps Windows paths intact and is also easy
 * for Windows PowerShell 5.1 to consume.
 */
public final class LocalDevelopmentConfiguration {
    private static final String CONFIG_RELATIVE_PATH = "deploy/local.env.properties";

    private final Path projectDirectory;
    private final Path configFile;
    private final Map<String, String> values;

    private LocalDevelopmentConfiguration(Path projectDirectory,
                                          Path configFile,
                                          Map<String, String> values) {
        this.projectDirectory = projectDirectory;
        this.configFile = configFile;
        this.values = values;
    }

    public static Optional<LocalDevelopmentConfiguration> discover() {
        String explicit = firstNonBlank(System.getProperty("APP_LOCAL_CONFIG"), System.getenv("APP_LOCAL_CONFIG"));
        if (explicit != null) {
            Path file = Path.of(explicit).toAbsolutePath().normalize();
            return Optional.of(load(file));
        }

        for (Path start : discoveryRoots()) {
            Path current = start.toAbsolutePath().normalize();
            for (int depth = 0; depth < 8 && current != null; depth++) {
                Path candidate = current.resolve(CONFIG_RELATIVE_PATH);
                if (Files.isRegularFile(candidate)) {
                    return Optional.of(load(candidate));
                }
                current = current.getParent();
            }
        }
        return Optional.empty();
    }

    private static LocalDevelopmentConfiguration load(Path configFile) {
        if (!Files.isRegularFile(configFile)) {
            throw new IllegalStateException("Local configuration does not exist: " + configFile);
        }
        Map<String, String> values = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(configFile, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                if (separator < 1) {
                    continue;
                }
                values.put(trimmed.substring(0, separator).trim(), trimmed.substring(separator + 1).trim());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read local configuration: " + configFile, exception);
        }
        Path project = configFile.getParent().getParent().toAbsolutePath().normalize();
        LocalDevelopmentConfiguration configuration = new LocalDevelopmentConfiguration(project, configFile, values);
        configuration.applyToSystemProperties();
        return configuration;
    }

    private void applyToSystemProperties() {
        values.forEach((key, value) -> {
            if (System.getProperty(key) == null && System.getenv(key) == null) {
                System.setProperty(key, value);
            }
        });
        String activeProfiles = value("SPRING_PROFILES_ACTIVE", "local");
        if (System.getProperty("spring.profiles.active") == null && System.getenv("SPRING_PROFILES_ACTIVE") == null) {
            System.setProperty("spring.profiles.active", activeProfiles);
        }
        ensureDevelopmentJwtSecret();
        System.setProperty("APP_LOCAL_CONFIG", configFile.toString());
    }

    /**
     * Restored development behavior: a local run without a configured JWT secret
     * receives an in-memory key. A configured environment or local property always wins.
     */
    private void ensureDevelopmentJwtSecret() {
        if (firstNonBlank(System.getProperty("APP_JWT_SECRET_BASE64"), System.getenv("APP_JWT_SECRET_BASE64")) != null) {
            return;
        }
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        System.setProperty("APP_JWT_SECRET_BASE64", Base64.getEncoder().encodeToString(bytes));
    }

    public String value(String key, String defaultValue) {
        String system = firstNonBlank(System.getProperty(key), System.getenv(key));
        if (system != null) {
            return system;
        }
        return values.getOrDefault(key, defaultValue);
    }

    public boolean booleanValue(String key, boolean defaultValue) {
        String value = value(key, Boolean.toString(defaultValue));
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    public Path projectDirectory() {
        return projectDirectory;
    }

    public Path configFile() {
        return configFile;
    }

    private static List<Path> discoveryRoots() {
        List<Path> roots = new ArrayList<>();
        roots.add(Path.of(System.getProperty("user.dir", ".")));
        try {
            roots.add(Path.of(LocalDevelopmentConfiguration.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()));
        } catch (URISyntaxException | NullPointerException ignored) {
            // The working-directory search remains available in unusual launchers.
        }
        return roots;
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }
}
