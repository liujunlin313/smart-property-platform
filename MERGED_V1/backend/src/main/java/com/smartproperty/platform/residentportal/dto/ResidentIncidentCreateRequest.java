package com.smartproperty.platform.residentportal.dto;
import jakarta.validation.constraints.*;
public record ResidentIncidentCreateRequest(@NotNull Long propertyUnitId,@NotBlank @Pattern(regexp="LEAK|ELECTRICITY|GAS|ELEVATOR|PUBLIC_FACILITY|NOISE|SECURITY|OTHER") String category,@NotBlank @Size(max=1000) String description){}
