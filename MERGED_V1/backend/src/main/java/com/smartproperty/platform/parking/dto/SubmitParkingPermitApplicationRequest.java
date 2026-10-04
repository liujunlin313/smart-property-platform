package com.smartproperty.platform.parking.dto;
import jakarta.validation.constraints.Max;import jakarta.validation.constraints.Min;import jakarta.validation.constraints.NotNull;
public record SubmitParkingPermitApplicationRequest(@NotNull Long propertyUnitId,@NotNull Long vehicleId,@NotNull @Min(1) @Max(12) Integer requestedMonths) {}
