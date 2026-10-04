package com.smartproperty.platform.map.model;

import java.math.BigDecimal;

/** MyBatis-only projection for DataScope-filtered community coordinates. */
public record ManagedCommunityLocationRow(Long communityId, String communityCode, String communityName,
                                          String address, BigDecimal longitude, BigDecimal latitude) { }
