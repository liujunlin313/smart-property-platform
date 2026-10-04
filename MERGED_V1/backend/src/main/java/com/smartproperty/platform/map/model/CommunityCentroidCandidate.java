package com.smartproperty.platform.map.model;

import java.math.BigDecimal;

/** Community centroid calculated from successfully located active buildings. */
public record CommunityCentroidCandidate(Long communityId, BigDecimal longitude, BigDecimal latitude) {
}
