package com.smartproperty.platform.asset.dto.response;

import java.time.LocalDateTime;

public record BuildingUnitResponse(Long buildingUnitId, Long buildingId, String buildingName,
        String unitNo, String unitName, String status, int version, int propertyCount,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
}
