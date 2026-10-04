package com.smartproperty.platform.asset.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record BuildingCreateRequest(@NotNull Long communityId,
        @NotBlank @Size(max=32) String buildingNo, @Size(max=100) String buildingName,
        @Min(1) @Max(200) Integer totalFloors,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @Pattern(regexp="ACTIVE|INACTIVE") String status,
        @Size(max=255) String changeReason) {
}
