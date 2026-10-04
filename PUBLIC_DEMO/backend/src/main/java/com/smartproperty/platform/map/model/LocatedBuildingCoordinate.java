package com.smartproperty.platform.map.model;

import java.math.BigDecimal;

/** Read-only coordinate projection used to surface same-community duplicate geocodes. */
public record LocatedBuildingCoordinate(
        Long communityId,
        String communityName,
        Long buildingId,
        String buildingNo,
        String buildingName,
        BigDecimal longitude,
        BigDecimal latitude
) {
    public String displayName() {
        return buildingName == null || buildingName.isBlank() ? buildingNo : buildingName;
    }
}
