package com.smartproperty.platform.asset.dto.request;

import jakarta.validation.constraints.*;

public record BuildingUnitCreateRequest(@NotNull Long buildingId,
        @NotBlank @Size(max=32) String unitNo, @Size(max=100) String unitName,
        @Pattern(regexp="ACTIVE|INACTIVE") String status,
        @Size(max=255) String changeReason) {
}
