package com.smartproperty.platform.asset.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CommunityUpdateRequest(@NotBlank @Size(max=32) String communityCode,
        @NotBlank @Size(max=100) String communityName, @NotBlank @Size(max=255) String addressLine,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @Min(1800) @Max(2200) Integer completionYear,
        @Pattern(regexp="ACTIVE|INACTIVE") String status, @NotNull Integer version,
        @Size(max=255) String changeReason) {
}
