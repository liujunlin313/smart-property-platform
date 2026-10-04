package com.smartproperty.platform.map.vo;

import java.math.BigDecimal;

public record CommunityMapPointVO(
        Long communityId, String communityCode, String communityName, String address,
        BigDecimal longitude, BigDecimal latitude, String coordinateSystem,
        Long buildingCount, Long propertyCount, Long occupiedPropertyCount,
        BigDecimal occupancyRate, boolean locationReady
) {
}
