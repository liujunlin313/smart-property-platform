package com.smartproperty.platform.property.vo;

import java.math.BigDecimal;
import java.util.List;

public record PropertyDetailVO(Long propertyUnitId, String propertyNo, String communityName,
                               String buildingName, String unitName, String layoutType,
                               BigDecimal grossArea, BigDecimal usableArea, String propertyUse,
                               String occupancyStatus, String status,
                               List<PropertyPartyVO> owners, List<PropertyPartyVO> residents,
                               List<MeterVO> activeMeters) {
}
