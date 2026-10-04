package com.smartproperty.platform.map.controller;

import com.smartproperty.platform.map.service.MapLocationSyncService;
import com.smartproperty.platform.map.service.MapSceneService;
import com.smartproperty.platform.map.service.MapPoiSearchService;
import com.smartproperty.platform.map.dto.PoiCommunityReverseRequest;
import com.smartproperty.platform.map.vo.MapPoiVO;
import com.smartproperty.platform.map.vo.NearbyPoiSearchVO;
import com.smartproperty.platform.map.vo.PoiCommunityReverseVO;
import jakarta.validation.Valid;
import com.smartproperty.platform.map.vo.MapLocationSyncResult;
import com.smartproperty.platform.map.vo.MapSceneVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/map")
public class MapController {
    private final MapLocationSyncService mapLocationSyncService;
    private final MapSceneService mapSceneService;
    private final MapPoiSearchService poiSearchService;

    public MapController(MapLocationSyncService mapLocationSyncService, MapSceneService mapSceneService,
                         MapPoiSearchService poiSearchService) {
        this.mapLocationSyncService = mapLocationSyncService;
        this.mapSceneService = mapSceneService;
        this.poiSearchService = poiSearchService;
    }

    @PostMapping("/locations/sync-missing")
    public MapLocationSyncResult syncMissingLocations() {
        return mapLocationSyncService.syncMissingLocations();
    }

    @GetMapping("/scene")
    public MapSceneVO getScene() {
        return mapSceneService.getScene();
    }

    @GetMapping("/search/nearby")
    public NearbyPoiSearchVO searchNearby(@RequestParam Long communityId, @RequestParam String keyword,
                                           @RequestParam(required = false) Integer radiusMeters) {
        return poiSearchService.searchNearby(communityId, keyword, radiusMeters);
    }

    @GetMapping("/search/poi-candidates")
    public java.util.List<MapPoiVO> searchPoiCandidates(@RequestParam String keyword) {
        return poiSearchService.searchPoiCandidates(keyword);
    }

    @PostMapping("/search/reverse-communities")
    public PoiCommunityReverseVO reverseCommunities(@Valid @RequestBody PoiCommunityReverseRequest request) {
        return poiSearchService.findManagedCommunities(request);
    }
}
