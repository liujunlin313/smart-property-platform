package com.smartproperty.platform.asset.model;

public record BuildingUnitEntity(Long buildingId, String unitNo, String unitName,
        String status, Integer version) {
}
