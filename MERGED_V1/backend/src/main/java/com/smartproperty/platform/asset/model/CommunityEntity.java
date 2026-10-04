package com.smartproperty.platform.asset.model;

import java.math.BigDecimal;

public record CommunityEntity(String communityCode, String communityName, String addressLine,
        BigDecimal longitude, BigDecimal latitude, Integer completionYear, String status,
        Integer version) {
}
