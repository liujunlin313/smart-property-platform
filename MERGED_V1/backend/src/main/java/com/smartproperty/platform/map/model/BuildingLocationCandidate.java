package com.smartproperty.platform.map.model;

import java.math.BigDecimal;

/** Database projection used only by the missing-location initialization workflow. */
public record BuildingLocationCandidate(
        Long buildingId,
        Long communityId,
        String communityName,
        String communityAddress,
        String buildingNo,
        String buildingName,
        BigDecimal longitude,
        BigDecimal latitude
) {
    public boolean hasValidLocation() {
        return longitude != null && latitude != null;
    }

    public String displayName() {
        return buildingName == null || buildingName.isBlank() ? buildingNo : buildingName;
    }
}
