package com.smartproperty.platform.map.vo;

import java.math.BigDecimal;
import java.util.List;

public record DuplicateCoordinateGroupVO(
        Long communityId,
        String communityName,
        BigDecimal longitude,
        BigDecimal latitude,
        List<BuildingCoordinateMemberVO> buildings
) {
}
