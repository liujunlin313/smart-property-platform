package com.smartproperty.platform.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap")
public record BootstrapProperties(
        String adminUsername,
        String adminPassword,
        String viewerUsername,
        String viewerPassword,
        boolean reconcileLocal
) {
}
