package com.smartproperty.platform.map.model;

import java.math.BigDecimal;

/** Internal MyBatis projection; not exposed directly to HTTP clients. */
public record CommunityMapPointRow(
        Long communityId, String communityCode, String communityName, String address,
        BigDecimal longitude, BigDecimal latitude, String coordinateSystem,
        Long buildingCount, Long propertyCount, Long occupiedPropertyCount
) {
}
