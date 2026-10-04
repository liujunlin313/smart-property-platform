package com.smartproperty.platform.property.vo;

import java.math.BigDecimal;

/** Scalar portion of a property detail query; related collections are assembled by Service. */
public record PropertyDetailBaseVO(Long propertyUnitId, String propertyNo, String communityName,
                                   String buildingName, String unitName, String layoutType,
                                   BigDecimal grossArea, BigDecimal usableArea, String propertyUse,
                                   String occupancyStatus, String status) {
}
