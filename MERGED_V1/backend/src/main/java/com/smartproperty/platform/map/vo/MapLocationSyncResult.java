package com.smartproperty.platform.map.vo;

import java.util.List;

public record MapLocationSyncResult(
        int totalBuildings,
        int alreadyLocated,
        int requested,
        int success,
        int failed,
        int updated,
        int distinctCoordinateCount,
        List<BuildingLocationSyncItemVO> results,
        List<DuplicateCoordinateGroupVO> duplicateCoordinateGroups,
        List<CommunityCentroidSyncVO> communityCentroids,
        List<String> warnings
) {
}
