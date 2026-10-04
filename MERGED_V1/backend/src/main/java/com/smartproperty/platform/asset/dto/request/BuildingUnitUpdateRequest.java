package com.smartproperty.platform.asset.dto.request;

import jakarta.validation.constraints.*;

public record BuildingUnitUpdateRequest(@NotNull Long buildingId,
        @NotBlank @Size(max=32) String unitNo, @Size(max=100) String unitName,
        @Pattern(regexp="ACTIVE|INACTIVE") String status, @NotNull Integer version,
        @Size(max=255) String changeReason) {
}
