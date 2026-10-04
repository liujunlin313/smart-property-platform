package com.smartproperty.platform.infrastructure.observability;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/** Sanitized browser diagnostics contract; it intentionally has no arbitrary payload map. */
public record ClientEventBatch(
        @NotEmpty @Size(max = 50) List<@Valid ClientEvent> events
) {
    public record ClientEvent(
            @Size(max = 24) String type,
            @Size(max = 100) String action,
            @Size(max = 240) String route,
            @Size(max = 600) String message,
            @Size(max = 2_000) String stack,
            @Size(max = 12) String method,
            @Size(max = 240) String url,
            Integer httpStatus,
            Instant timestamp
    ) {
    }
}
