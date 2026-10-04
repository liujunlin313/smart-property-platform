package com.smartproperty.platform.map.vo;

import java.math.BigDecimal;

public record BuildingLocationSyncItemVO(
        Long buildingId,
        String communityName,
        String buildingName,
        String queryAddress,
        String status,
        String message,
        BigDecimal longitude,
        BigDecimal latitude,
        String coordinateSystem,
        String formattedAddress,
        String level
) {
}
