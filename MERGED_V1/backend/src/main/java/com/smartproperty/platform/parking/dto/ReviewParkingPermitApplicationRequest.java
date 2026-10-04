package com.smartproperty.platform.parking.dto;
import jakarta.validation.constraints.NotBlank;import jakarta.validation.constraints.Size;
public record ReviewParkingPermitApplicationRequest(@NotBlank String decision,@Size(max=300) String rejectionReason) {}
