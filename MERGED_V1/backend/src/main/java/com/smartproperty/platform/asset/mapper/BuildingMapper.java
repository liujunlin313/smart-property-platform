package com.smartproperty.platform.asset.mapper;

import com.smartproperty.platform.asset.dto.response.BuildingResponse;
import com.smartproperty.platform.asset.model.BuildingEntity;
import com.smartproperty.platform.access.CommunityDataScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface BuildingMapper {
    List<BuildingResponse> findAll(@Param("communityId") Long communityId, @Param("status") String status, @Param("scope") CommunityDataScope scope);
    BuildingResponse findById(@Param("buildingId") Long buildingId, @Param("scope") CommunityDataScope scope);
    int insert(@Param("entity") BuildingEntity entity);
    int update(@Param("buildingId") Long buildingId, @Param("entity") BuildingEntity entity);
    Long lastInsertId();
}
