package com.smartproperty.platform.notification.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record NotificationTargetRequest(@NotBlank String targetType, @NotEmpty List<Long> targetIds) { }
