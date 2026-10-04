package com.smartproperty.platform.map.vo;

import java.math.BigDecimal;

public record CommunityCentroidSyncVO(Long communityId, BigDecimal longitude, BigDecimal latitude, String status) {
}
