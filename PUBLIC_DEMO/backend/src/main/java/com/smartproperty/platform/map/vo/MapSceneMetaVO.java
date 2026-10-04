package com.smartproperty.platform.map.vo;

import java.time.OffsetDateTime;

public record MapSceneMetaVO(
        String coordinateSystem,
        OffsetDateTime generatedAt,
        int communityVisibleMaxZoom,
        int buildingVisibleMinZoom,
        long locatedCommunityCount,
        long locatedBuildingCount
) {
}
