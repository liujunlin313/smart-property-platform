package com.smartproperty.platform.map.service.impl;

import com.smartproperty.platform.access.AuthorizationService;
import com.smartproperty.platform.access.CommunityDataScope;
import com.smartproperty.platform.access.DataScopeService;
import com.smartproperty.platform.access.PermissionCodes;
import com.smartproperty.platform.common.exception.BusinessException;
import com.smartproperty.platform.infrastructure.map.exception.MapProviderNotConfiguredException;
import com.smartproperty.platform.infrastructure.map.exception.MapProviderRequestException;
import com.smartproperty.platform.infrastructure.map.model.GeoModels.CoordinateSystem;
import com.smartproperty.platform.infrastructure.map.model.GeoModels.GeoPoint;
import com.smartproperty.platform.infrastructure.map.model.GeoModels.NearbyPoiSearchRequest;
import com.smartproperty.platform.infrastructure.map.model.GeoModels.PoiItem;
import com.smartproperty.platform.infrastructure.map.model.GeoModels.PoiSearchRequest;
import com.smartproperty.platform.infrastructure.map.service.GeoLocationService;
import com.smartproperty.platform.map.dto.PoiCommunityReverseRequest;
import com.smartproperty.platform.map.mapper.MapSearchMapper;
import com.smartproperty.platform.map.model.ManagedCommunityLocationRow;
import com.smartproperty.platform.map.service.MapPoiSearchService;
import com.smartproperty.platform.map.vo.ManagedCommunityDistanceVO;
import com.smartproperty.platform.map.vo.MapPoiVO;
import com.smartproperty.platform.map.vo.NearbyPoiSearchVO;
import com.smartproperty.platform.map.vo.PoiCommunityReverseVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
public class MapPoiSearchServiceImpl implements MapPoiSearchService {
    private static final int DEFAULT_RADIUS_METERS = 3000;
    private static final int MAX_RADIUS_METERS = 50000;
    private final AuthorizationService authorizationService;
    private final DataScopeService dataScopeService;
    private final MapSearchMapper mapper;
    private final GeoLocationService geoLocationService;

    public MapPoiSearchServiceImpl(AuthorizationService authorizationService, DataScopeService dataScopeService,
                                   MapSearchMapper mapper, GeoLocationService geoLocationService) {
        this.authorizationService = authorizationService;
        this.dataScopeService = dataScopeService;
        this.mapper = mapper;
        this.geoLocationService = geoLocationService;
    }

    @Override
    public NearbyPoiSearchVO searchNearby(Long communityId, String keyword, Integer radiusMeters) {
        authorizationService.requirePermission(PermissionCodes.MAP_VIEW);
        dataScopeService.requireCommunity(communityId);
        String normalizedKeyword = requireKeyword(keyword);
        ManagedCommunityLocationRow community = mapper.findVisibleCommunity(communityId, dataScopeService.currentCommunityScope());
        if (community == null || community.longitude() == null || community.latitude() == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "COMMUNITY_LOCATION_NOT_FOUND", "该小区暂未维护可用中心坐标");
        }
        int radius = normalizeRadius(radiusMeters);
        try {
            List<MapPoiVO> pois = geoLocationService.searchNearbyPoi(new NearbyPoiSearchRequest(
                    point(community.longitude(), community.latitude()), normalizedKeyword, radius, 1, 25))
                    .items().stream().filter(item -> item.location() != null).map(this::toPoi).toList();
            return new NearbyPoiSearchVO(community.communityId(), community.communityName(), normalizedKeyword, radius, pois);
        } catch (MapProviderNotConfiguredException | MapProviderRequestException exception) {
            throw providerUnavailable();
        }
    }

    @Override
    public List<MapPoiVO> searchPoiCandidates(String keyword) {
        authorizationService.requirePermission(PermissionCodes.MAP_VIEW);
        try {
            return geoLocationService.searchPoi(new PoiSearchRequest(requireKeyword(keyword), null, 1, 25)).items().stream()
                    .filter(item -> item.location() != null).map(this::toPoi).toList();
        } catch (MapProviderNotConfiguredException | MapProviderRequestException exception) {
            throw providerUnavailable();
        }
    }

    @Override
    public PoiCommunityReverseVO findManagedCommunities(PoiCommunityReverseRequest request) {
        authorizationService.requirePermission(PermissionCodes.MAP_VIEW);
        MapPoiVO poi;
        try {
            poi = geoLocationService.searchPoi(new PoiSearchRequest(requireKeyword(request.keyword()), null, 1, 25)).items().stream()
                    .filter(item -> request.poiId().trim().equals(item.poiId())).filter(item -> item.location() != null)
                    .map(this::toPoi).findFirst().orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
                            "POI_SELECTION_EXPIRED", "所选 POI 已失效，请重新搜索后选择"));
        } catch (MapProviderNotConfiguredException | MapProviderRequestException exception) {
            throw providerUnavailable();
        }
        CommunityDataScope scope = dataScopeService.currentCommunityScope();
        List<ManagedCommunityDistanceVO> communities = mapper.findVisibleCommunityLocations(scope).stream()
                .map(community -> new ManagedCommunityDistanceVO(community.communityId(), community.communityCode(),
                        community.communityName(), community.address(), distanceMeters(poi.longitude(), poi.latitude(),
                        community.longitude(), community.latitude())))
                .sorted(Comparator.comparingLong(ManagedCommunityDistanceVO::straightLineDistanceMeters)).toList();
        return new PoiCommunityReverseVO(poi, communities);
    }

    private MapPoiVO toPoi(PoiItem item) {
        return new MapPoiVO(item.poiId(), item.name(), item.type(), item.address(), item.location().longitude(), item.location().latitude());
    }
    private GeoPoint point(BigDecimal longitude, BigDecimal latitude) { return new GeoPoint(longitude, latitude, CoordinateSystem.GCJ_02); }
    private String requireKeyword(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank() || normalized.length() > 80) throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_POI_KEYWORD", "请输入 1 到 80 个字符的 POI 关键字");
        return normalized;
    }
    private int normalizeRadius(Integer value) { return value == null ? DEFAULT_RADIUS_METERS : Math.max(100, Math.min(value, MAX_RADIUS_METERS)); }
    private long distanceMeters(BigDecimal lng1, BigDecimal lat1, BigDecimal lng2, BigDecimal lat2) {
        double earth = 6_371_000d, dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue()), dLng = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1.doubleValue())) * Math.cos(Math.toRadians(lat2.doubleValue())) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return BigDecimal.valueOf(earth * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))).setScale(0, RoundingMode.HALF_UP).longValue();
    }
    private BusinessException providerUnavailable() { return new BusinessException(HttpStatus.BAD_GATEWAY, "MAP_PROVIDER_UNAVAILABLE", "地图服务暂时不可用，请稍后重试"); }
}
