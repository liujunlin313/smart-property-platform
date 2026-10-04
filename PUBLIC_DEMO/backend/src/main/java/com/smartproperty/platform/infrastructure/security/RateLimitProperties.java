package com.smartproperty.platform.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Local in-memory request guard. It intentionally does not use forwarded client IP headers. */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        int windowSeconds,
        int authPerWindow,
        int readPerWindow,
        int expensiveReadPerWindow,
        int writePerWindow,
        int uploadPerWindow) {
}
