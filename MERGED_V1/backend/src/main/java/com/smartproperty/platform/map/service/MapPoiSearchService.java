package com.smartproperty.platform.map.service;

import com.smartproperty.platform.map.dto.PoiCommunityReverseRequest;
import com.smartproperty.platform.map.vo.MapPoiVO;
import com.smartproperty.platform.map.vo.NearbyPoiSearchVO;
import com.smartproperty.platform.map.vo.PoiCommunityReverseVO;
import java.util.List;

public interface MapPoiSearchService {
    NearbyPoiSearchVO searchNearby(Long communityId, String keyword, Integer radiusMeters);
    List<MapPoiVO> searchPoiCandidates(String keyword);
    PoiCommunityReverseVO findManagedCommunities(PoiCommunityReverseRequest request);
}
