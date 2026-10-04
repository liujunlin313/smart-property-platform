package com.smartproperty.platform.map.mapper;

import com.smartproperty.platform.access.CommunityDataScope;
import com.smartproperty.platform.map.model.BuildingMapPointRow;
import com.smartproperty.platform.map.model.CommunityMapPointRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MapSceneMapper {
    List<CommunityMapPointRow> selectCommunityMapPoints(@Param("scope") CommunityDataScope scope);
    List<BuildingMapPointRow> selectBuildingMapPoints(@Param("scope") CommunityDataScope scope);
}
