package com.smartproperty.platform.map.model;

import java.math.BigDecimal;

/** Internal MyBatis projection; keeps scene aggregation away from controller code. */
public record BuildingMapPointRow(
        Long buildingId, Long communityId, String communityName,
        String buildingNo, String buildingName, String fullAddress,
        BigDecimal longitude, BigDecimal latitude,
        Integer totalFloors, String locationSource,
        Long unitCount, Long propertyCount, Long occupiedPropertyCount
) {
}
