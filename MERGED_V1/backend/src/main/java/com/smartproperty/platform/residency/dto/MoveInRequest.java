package com.smartproperty.platform.residency.dto;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
public record MoveInRequest(@NotNull Long personId,@NotNull Long propertyUnitId,@NotNull LocalDate startDate,LocalDate expectedEndDate,@NotNull MoveInType moveInType,Boolean parkingApplicationEligible,String parkingIneligibleReasonCode,String parkingEligibilityRemark) { public enum MoveInType { OWNER_OCCUPY, TENANT } }
