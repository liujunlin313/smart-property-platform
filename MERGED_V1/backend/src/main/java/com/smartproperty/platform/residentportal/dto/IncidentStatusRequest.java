package com.smartproperty.platform.residentportal.dto;
import jakarta.validation.constraints.*;
public record IncidentStatusRequest(@NotBlank @Pattern(regexp="PROCESSING|RESOLVED") String status){}
