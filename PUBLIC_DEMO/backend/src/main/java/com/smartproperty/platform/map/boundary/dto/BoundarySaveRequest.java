package com.smartproperty.platform.map.boundary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** A real manually calibrated GCJ-02 GeoJSON polygon. */
public record BoundarySaveRequest(@NotBlank String geoJson, @NotNull Long version) { }
