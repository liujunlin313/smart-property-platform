package com.smartproperty.platform.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        String jwtSecretBase64,
        String issuer,
        String audience,
        long accessTokenMinutes,
        long refreshTokenHours,
        boolean refreshCookieSecure,
        List<String> allowedOrigins
) {
}
