package com.smartproperty.platform.map.boundary.vo;

import java.time.OffsetDateTime;

public record SpatialBoundaryVO(String subjectType, Long subjectId, String geoJson, String coordinateSystem,
                                String source, String status, Long version, OffsetDateTime verifiedAt) { }
