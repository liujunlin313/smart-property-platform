package com.smartproperty.platform.resident.vo;

public record ResidentSummaryVO(long activeResidentCount, long responsibleCount, long ownerOccupiedCount,
                                long tenantOccupiedCount, long availableForRentCount, long vacantCount) { }
