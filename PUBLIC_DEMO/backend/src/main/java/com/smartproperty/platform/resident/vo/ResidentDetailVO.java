package com.smartproperty.platform.resident.vo;

import java.time.LocalDate;

public record ResidentDetailVO(Long residenceId, Long personId, String fullName, String maskedPhone,
                               String communityName, String buildingName, String unitName, String propertyNo,
                               String fullLocation, String residenceRole, String relationToResponsible,
                               String responsibleName, String ownerName, String occupancyStatus,
                               LocalDate startDate) { }
