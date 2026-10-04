package com.smartproperty.platform.asset.dto.response;

public record AssetDashboardResponse(int communityCount, int buildingCount,
        int buildingUnitCount, int propertyCount, int activePropertyCount, int draftPropertyCount) {
}
