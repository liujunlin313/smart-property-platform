package com.smartproperty.platform.asset.mapper;

import com.smartproperty.platform.asset.dto.response.BuildingUnitResponse;
import com.smartproperty.platform.asset.model.BuildingUnitEntity;
import com.smartproperty.platform.access.CommunityDataScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface BuildingUnitMapper {
    List<BuildingUnitResponse> findAll(@Param("buildingId") Long buildingId, @Param("status") String status, @Param("scope") CommunityDataScope scope);
    BuildingUnitResponse findById(@Param("buildingUnitId") Long buildingUnitId, @Param("scope") CommunityDataScope scope);
    int insert(@Param("entity") BuildingUnitEntity entity);
    int update(@Param("buildingUnitId") Long buildingUnitId, @Param("entity") BuildingUnitEntity entity);
    Long lastInsertId();
}
