package com.smartproperty.platform.asset.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PropertyUnitResponse(Long propertyUnitId, Long buildingUnitId, Long buildingId,
        Long communityId, Long layoutTemplateId, String communityName, String buildingNo,
        String buildingName, String unitNo, String unitName, String propertyNo, Integer floorNo,
        BigDecimal grossArea, BigDecimal usableArea, String propertyUse, String status,
        String occupancyStatus, int version, String layoutCode, String layoutName,
        String layoutType, Long floorPlanFileId, String floorPlanContentType, String floorPlanSource,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
}
