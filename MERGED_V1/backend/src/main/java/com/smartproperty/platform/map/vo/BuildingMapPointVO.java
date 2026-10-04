package com.smartproperty.platform.map.vo;

import java.math.BigDecimal;

public record BuildingMapPointVO(
        Long buildingId, Long communityId, String communityName,
        String buildingNo, String buildingName, String fullAddress,
        BigDecimal longitude, BigDecimal latitude, String coordinateSystem,
        Integer totalFloors, Long unitCount, Long propertyCount,
        Long occupiedPropertyCount, Long vacantPropertyCount, BigDecimal occupancyRate,
        String locationSource, boolean locationReady
) {
}
