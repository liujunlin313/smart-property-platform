package com.smartproperty.platform.map.service.impl;

import com.smartproperty.platform.access.AuthorizationService;
import com.smartproperty.platform.access.CommunityDataScope;
import com.smartproperty.platform.access.DataScopeService;
import com.smartproperty.platform.access.PermissionCodes;
import com.smartproperty.platform.map.mapper.MapSceneMapper;
import com.smartproperty.platform.map.model.BuildingMapPointRow;
import com.smartproperty.platform.map.model.CommunityMapPointRow;
import com.smartproperty.platform.map.service.MapSceneService;
import com.smartproperty.platform.map.vo.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class MapSceneServiceImpl implements MapSceneService {
    private static final int COMMUNITY_VISIBLE_MAX_ZOOM = 14;
    private static final int BUILDING_VISIBLE_MIN_ZOOM = 15;
    private final MapSceneMapper mapper;
    private final AuthorizationService authorizationService;
    private final DataScopeService dataScopeService;

    public MapSceneServiceImpl(MapSceneMapper mapper, AuthorizationService authorizationService, DataScopeService dataScopeService) {
        this.mapper = mapper;
        this.authorizationService = authorizationService;
        this.dataScopeService = dataScopeService;
    }

    @Override
    public MapSceneVO getScene() {
        authorizationService.requirePermission(PermissionCodes.MAP_VIEW);
        CommunityDataScope scope = dataScopeService.currentCommunityScope();
        List<CommunityMapPointVO> communities = mapper.selectCommunityMapPoints(scope).stream().map(this::toCommunity).toList();
        List<BuildingMapPointVO> buildings = mapper.selectBuildingMapPoints(scope).stream().map(this::toBuilding).toList();
        return new MapSceneVO(communities, buildings, new MapSceneMetaVO("GCJ_02", OffsetDateTime.now(),
                COMMUNITY_VISIBLE_MAX_ZOOM, BUILDING_VISIBLE_MIN_ZOOM,
                communities.stream().filter(CommunityMapPointVO::locationReady).count(),
                buildings.stream().filter(BuildingMapPointVO::locationReady).count()));
    }

    private CommunityMapPointVO toCommunity(CommunityMapPointRow row) {
        return new CommunityMapPointVO(row.communityId(), row.communityCode(), row.communityName(), row.address(),
                row.longitude(), row.latitude(), normalizeCoordinateSystem(row.coordinateSystem()),
                nonNull(row.buildingCount()), nonNull(row.propertyCount()), nonNull(row.occupiedPropertyCount()),
                percentage(row.occupiedPropertyCount(), row.propertyCount()), ready(row.longitude(), row.latitude()));
    }

    private BuildingMapPointVO toBuilding(BuildingMapPointRow row) {
        long propertyCount = nonNull(row.propertyCount());
        long occupied = nonNull(row.occupiedPropertyCount());
        return new BuildingMapPointVO(row.buildingId(), row.communityId(), row.communityName(), row.buildingNo(),
                row.buildingName() == null || row.buildingName().isBlank() ? row.buildingNo() : row.buildingName(),
                row.fullAddress(), row.longitude(), row.latitude(), "GCJ_02", row.totalFloors(), nonNull(row.unitCount()),
                propertyCount, occupied, Math.max(0, propertyCount - occupied), percentage(occupied, propertyCount),
                row.locationSource(), ready(row.longitude(), row.latitude()));
    }

    private long nonNull(Long value) { return value == null ? 0L : value; }
    private BigDecimal percentage(Long numerator, Long denominator) { return percentage(nonNull(numerator), nonNull(denominator)); }
    private BigDecimal percentage(long numerator, long denominator) {
        return denominator == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }
    private boolean ready(BigDecimal longitude, BigDecimal latitude) { return longitude != null && latitude != null; }
    private String normalizeCoordinateSystem(String value) { return value == null || value.isBlank() ? "GCJ_02" : "GCJ_02"; }
}
