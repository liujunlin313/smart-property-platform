package com.smartproperty.platform.asset.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CommunityResponse(Long communityId, String communityCode, String communityName,
        String addressLine, BigDecimal longitude, BigDecimal latitude, String locationSource,
        String coordinateSystem, String amapPoiId, LocalDateTime locationVerifiedAt,
        Integer completionYear, String status, int version, int buildingCount, int propertyCount,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
}
