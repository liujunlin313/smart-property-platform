package com.smartproperty.platform.asset.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PropertyUnitCreateRequest(@NotNull Long buildingUnitId, Long layoutTemplateId,
        @NotBlank @Size(max=32) String propertyNo, @Min(-20) @Max(200) Integer floorNo,
        @DecimalMin("0.01") BigDecimal grossArea, @DecimalMin("0.01") BigDecimal usableArea,
        @Pattern(regexp="RESIDENTIAL|COMMERCIAL|OFFICE|OTHER") String propertyUse,
        @Pattern(regexp="DRAFT|ACTIVE|INACTIVE|ARCHIVED") String status,
        @Pattern(regexp="UNKNOWN|VACANT|OWNER_OCCUPIED") String occupancyStatus,
        @Size(max=255) String changeReason) {
}
