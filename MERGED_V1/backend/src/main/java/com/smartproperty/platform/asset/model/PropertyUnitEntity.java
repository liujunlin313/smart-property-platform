package com.smartproperty.platform.asset.model;

import java.math.BigDecimal;

public record PropertyUnitEntity(Long buildingUnitId, Long layoutTemplateId, String propertyNo,
        Integer floorNo, BigDecimal grossArea, BigDecimal usableArea, String propertyUse,
        String status, String occupancyStatus, Integer version) {
}
