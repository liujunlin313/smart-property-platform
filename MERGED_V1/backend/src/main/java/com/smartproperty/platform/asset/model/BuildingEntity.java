package com.smartproperty.platform.asset.model;

import java.math.BigDecimal;

public record BuildingEntity(Long communityId, String buildingNo, String buildingName,
        Integer totalFloors, BigDecimal longitude, BigDecimal latitude, String status,
        Integer version) {
}
