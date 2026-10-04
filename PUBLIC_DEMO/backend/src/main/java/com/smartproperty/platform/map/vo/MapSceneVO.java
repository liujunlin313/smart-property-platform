package com.smartproperty.platform.map.vo;

import java.util.List;

public record MapSceneVO(List<CommunityMapPointVO> communities, List<BuildingMapPointVO> buildings, MapSceneMetaVO meta) {
}
