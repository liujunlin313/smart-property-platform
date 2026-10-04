package com.smartproperty.platform.common.logging.flow;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.logging")
public record FlowLoggingProperties(boolean flowEnabled, int sqlMaxLength) {
    public int safeSqlMaxLength() {
        return sqlMaxLength <= 0 ? 420 : Math.min(sqlMaxLength, 800);
    }
}
