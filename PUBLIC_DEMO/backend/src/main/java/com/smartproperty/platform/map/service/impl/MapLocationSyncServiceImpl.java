package com.smartproperty.platform.map.service.impl;

import com.smartproperty.platform.access.AuthorizationService;
import com.smartproperty.platform.infrastructure.map.model.GeoModels.CoordinateSystem;
import com.smartproperty.platform.infrastructure.map.model.GeoModels.GeocodeRequest;
import com.smartproperty.platform.infrastructure.map.model.GeoModels.GeocodeResult;
import com.smartproperty.platform.infrastructure.map.service.GeoLocationService;
import com.smartproperty.platform.map.mapper.MapLocationMapper;
import com.smartproperty.platform.map.model.BuildingLocationCandidate;
import com.smartproperty.platform.map.model.LocatedBuildingCoordinate;
import com.smartproperty.platform.map.service.BuildingGeocodeAddressAssembler;
import com.smartproperty.platform.map.service.MapLocationSyncService;
import com.smartproperty.platform.map.vo.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MapLocationSyncServiceImpl implements MapLocationSyncService {
    /** Keeps one batch safely below the configured Web Service key's QPS ceiling. */
    private static final long AMAP_REQUEST_INTERVAL_MILLIS = 300L;
    private final MapLocationMapper mapper;
    private final GeoLocationService geoLocationService;
    private final BuildingGeocodeAddressAssembler addressAssembler;
    private final AuthorizationService authorizationService;
    private long lastAmapRequestAt;

    public MapLocationSyncServiceImpl(MapLocationMapper mapper,
                                      GeoLocationService geoLocationService,
                                      BuildingGeocodeAddressAssembler addressAssembler,
                                      AuthorizationService authorizationService) {
        this.mapper = mapper;
        this.geoLocationService = geoLocationService;
        this.addressAssembler = addressAssembler;
        this.authorizationService = authorizationService;
    }

    @Override
    public MapLocationSyncResult syncMissingLocations() {
        authorizationService.requireSystemAdmin();
        List<BuildingLocationCandidate> candidates = mapper.selectActiveBuildingsForLocationSync();
        List<BuildingLocationSyncItemVO> results = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int alreadyLocated = 0;
        int requested = 0;
        int success = 0;
        int failed = 0;
        int updated = 0;

        for (BuildingLocationCandidate candidate : candidates) {
            if (candidate.hasValidLocation()) {
                alreadyLocated++;
                results.add(item(candidate, null, "SKIPPED", "已有坐标，未覆盖"));
                continue;
            }
            String address = addressAssembler.assemble(candidate);
            if (address.isBlank()) {
                failed++;
                results.add(item(candidate, null, "FAILED", "无法根据现有小区/楼栋资料组装定位地址"));
                continue;
            }
            requested++;
            try {
                awaitNextAmapRequest();
                GeocodeResult geocode = geoLocationService.geocode(new GeocodeRequest(address, null));
                if (geocode.location() == null) {
                    failed++;
                    results.add(item(candidate, null, "FAILED", "高德未返回有效坐标"));
                    continue;
                }
                success++;
                int affected = mapper.updateBuildingLocationIfMissing(candidate.buildingId(),
                        geocode.location().longitude(), geocode.location().latitude());
                if (affected == 1) {
                    updated++;
                    results.add(item(candidate, geocode, "UPDATED", "已写入高德地理编码坐标"));
                } else {
                    results.add(item(candidate, geocode, "SKIPPED", "坐标已被其他操作补全，未覆盖"));
                }
            } catch (RuntimeException exception) {
                failed++;
                results.add(item(candidate, null, "FAILED", safeMessage(exception)));
            }
        }

        List<DuplicateCoordinateGroupVO> duplicates = duplicateGroups();
        if (!duplicates.isEmpty()) {
            warnings.add("发现同一小区内完全相同的楼栋坐标；可能是地理编码只解析到小区级位置，系统未对坐标作任何人工偏移。");
        }
        List<CommunityCentroidSyncVO> centroids = syncMissingCommunityCentroids();
        int distinctCoordinateCount = (int) mapper.selectLocatedActiveBuildings().stream()
                .map(coordinate -> new CoordinateKey(coordinate.longitude(), coordinate.latitude()))
                .distinct().count();

        return new MapLocationSyncResult(candidates.size(), alreadyLocated, requested, success, failed, updated,
                distinctCoordinateCount, List.copyOf(results), duplicates, centroids, List.copyOf(warnings));
    }

    private List<CommunityCentroidSyncVO> syncMissingCommunityCentroids() {
        return mapper.selectMissingCommunityCentroidsFromBuildings().stream()
                .map(centroid -> new CommunityCentroidSyncVO(centroid.communityId(), centroid.longitude(), centroid.latitude(),
                        mapper.updateCommunityCentroidIfMissing(centroid.communityId(), centroid.longitude(), centroid.latitude()) == 1
                                ? "UPDATED" : "SKIPPED"))
                .toList();
    }

    private List<DuplicateCoordinateGroupVO> duplicateGroups() {
        return mapper.selectLocatedActiveBuildings().stream()
                .collect(Collectors.groupingBy(value -> new DuplicateKey(value.communityId(), value.longitude(), value.latitude()), LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .filter(group -> group.size() > 1)
                .map(group -> {
                    LocatedBuildingCoordinate first = group.getFirst();
                    return new DuplicateCoordinateGroupVO(first.communityId(), first.communityName(), first.longitude(), first.latitude(),
                            group.stream().map(value -> new BuildingCoordinateMemberVO(value.buildingId(), value.displayName())).toList());
                })
                .toList();
    }

    private BuildingLocationSyncItemVO item(BuildingLocationCandidate candidate, GeocodeResult result, String status, String message) {
        return new BuildingLocationSyncItemVO(candidate.buildingId(), candidate.communityName(), candidate.displayName(),
                addressAssembler.assemble(candidate), status, message,
                result == null ? candidate.longitude() : result.location().longitude(),
                result == null ? candidate.latitude() : result.location().latitude(),
                result == null ? null : CoordinateSystem.GCJ_02.name(),
                result == null ? null : result.formattedAddress(), result == null ? null : result.level());
    }

    private String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank() ? "高德定位请求失败" : exception.getMessage();
    }

    private synchronized void awaitNextAmapRequest() {
        long elapsed = System.currentTimeMillis() - lastAmapRequestAt;
        long waitMillis = Math.max(0L, AMAP_REQUEST_INTERVAL_MILLIS - elapsed);
        if (waitMillis > 0) {
            try {
                Thread.sleep(waitMillis);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("地图定位任务已被中断", exception);
            }
        }
        lastAmapRequestAt = System.currentTimeMillis();
    }

    private record CoordinateKey(BigDecimal longitude, BigDecimal latitude) {
        private CoordinateKey {
            longitude = normalize(longitude);
            latitude = normalize(latitude);
        }
        private static BigDecimal normalize(BigDecimal value) {
            return value == null ? null : value.stripTrailingZeros();
        }
    }

    private record DuplicateKey(Long communityId, BigDecimal longitude, BigDecimal latitude) {
        private DuplicateKey {
            longitude = CoordinateKey.normalize(longitude);
            latitude = CoordinateKey.normalize(latitude);
        }
    }
}
