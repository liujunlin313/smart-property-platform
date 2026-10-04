package com.smartproperty.platform.map.vo;

import java.util.List;

public record PoiCommunityReverseVO(MapPoiVO poi, List<ManagedCommunityDistanceVO> communities) { }
