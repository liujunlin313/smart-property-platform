package com.smartproperty.platform.auth.model;

import java.time.LocalDateTime;

public record AccountRow(
        Long accountId,
        Long personId,
        String username,
        String passwordHash,
        String status,
        int failedLoginAttempts,
        LocalDateTime lockedUntil,
        int tokenVersion,
        LocalDateTime lastLoginAt
) {
}
