package com.smartproperty.platform.map.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PoiCommunityReverseRequest(@NotBlank @Size(max = 80) String keyword,
                                         @NotBlank @Size(max = 120) String poiId) { }
