package com.smartproperty.platform.residency.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CreatePersonRequest(@NotBlank @Size(max=100) String fullName,@Size(max=16) String gender,@Size(max=32) String phone) { }
