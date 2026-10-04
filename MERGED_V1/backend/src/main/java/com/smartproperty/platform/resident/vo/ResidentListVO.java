package com.smartproperty.platform.resident.vo;

import java.time.LocalDate;

public record ResidentListVO(Long residenceId, Long personId, String fullName, String maskedPhone,
                             Long communityId, String communityName, String buildingName, String unitName,
                             String propertyNo, String fullLocation, String residenceRole,
                             String relationToResponsible, String occupancyStatus, LocalDate startDate) { }
