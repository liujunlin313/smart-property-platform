package com.smartproperty.platform.map.boundary.vo;

import java.math.BigDecimal;

public record SpatialSearchResultVO(String subjectType, Long subjectId, Long communityId, String label,
                                    String subtitle, BigDecimal longitude, BigDecimal latitude,
                                    boolean boundaryReady) { }
