package com.smartproperty.platform.notification.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record NotificationSendRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 20000) String content,
        @NotBlank String category,
        Long feeTypeId,
        @NotEmpty List<@Valid NotificationTargetRequest> toTargets,
        List<@Valid NotificationTargetRequest> ccTargets
) { }
