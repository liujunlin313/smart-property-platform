package com.smartproperty.platform.auth.model;

import java.time.LocalDateTime;

public record RefreshSessionRow(
        Long refreshTokenSessionId,
        Long accountId,
        String tokenHash,
        LocalDateTime expiresAt,
        LocalDateTime revokedAt
) {
}
