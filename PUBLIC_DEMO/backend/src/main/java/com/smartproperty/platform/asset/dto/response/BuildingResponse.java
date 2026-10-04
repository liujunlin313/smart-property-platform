package com.smartproperty.platform.asset.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BuildingResponse(Long buildingId, Long communityId, String communityName,
        String buildingNo, String buildingName, Integer totalFloors, BigDecimal longitude,
        BigDecimal latitude, String locationSource, String amapPoiId,
        LocalDateTime locationVerifiedAt, String status, int version, int unitCount,
        int propertyCount, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
