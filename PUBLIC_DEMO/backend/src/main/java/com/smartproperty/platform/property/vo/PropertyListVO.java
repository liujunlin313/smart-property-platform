package com.smartproperty.platform.property.vo;

import java.math.BigDecimal;

public record PropertyListVO(Long propertyUnitId, String propertyNo, String communityName,
                             String buildingName, String unitName, String layoutType,
                             BigDecimal grossArea, String occupancyStatus) {
}
