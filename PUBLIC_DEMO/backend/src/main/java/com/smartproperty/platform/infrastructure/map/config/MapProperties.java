package com.smartproperty.platform.infrastructure.map.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.map")
public record MapProperties(String provider, Amap amap) {
    public record Amap(String webServiceKey, String webServicePrivateKey, String baseUrl) {
    }

    public boolean configured() { return amap != null && webServiceKey() != null && !webServiceKey().isBlank(); }
    public String webServiceKey() { return amap == null ? null : amap.webServiceKey(); }
    public String webServicePrivateKey() { return amap == null ? null : amap.webServicePrivateKey(); }
    public String baseUrl() { return amap == null ? null : amap.baseUrl(); }
}
