package com.smartproperty.platform.map.service;

import com.smartproperty.platform.map.model.BuildingLocationCandidate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Pure address assembly; provider URL and persistence details deliberately stay elsewhere. */
@Component
public class BuildingGeocodeAddressAssembler {
    public String assemble(BuildingLocationCandidate candidate) {
        List<String> parts = new ArrayList<>();
        add(parts, candidate.communityAddress());
        add(parts, candidate.communityName());
        add(parts, candidate.displayName());
        return String.join("", parts);
    }

    private void add(List<String> parts, String value) {
        if (value != null && !value.isBlank() && parts.stream().noneMatch(value.trim()::equals)) {
            parts.add(value.trim());
        }
    }
}
