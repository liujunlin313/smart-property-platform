package com.smartproperty.platform.parking.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CreateVehicleRequest(@NotBlank @Size(max=32) String plateNumber, @Size(max=32) String vehicleType, @Size(max=64) String brand, @Size(max=32) String color) {}
