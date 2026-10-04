package com.smartproperty.platform.map.vo;

import java.util.List;

public record NearbyPoiSearchVO(Long communityId, String communityName, String keyword,
                                int radiusMeters, List<MapPoiVO> pois) { }
