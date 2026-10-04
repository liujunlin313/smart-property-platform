package com.smartproperty.platform.asset.mapper;

import com.smartproperty.platform.asset.dto.response.PropertyUnitResponse;
import com.smartproperty.platform.asset.model.PropertyUnitEntity;
import com.smartproperty.platform.access.CommunityDataScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface PropertyUnitMapper {
    List<PropertyUnitResponse> findAll(@Param("communityId") Long communityId,
            @Param("buildingId") Long buildingId, @Param("buildingUnitId") Long buildingUnitId,
            @Param("layoutTemplateId") Long layoutTemplateId, @Param("keyword") String keyword,
            @Param("status") String status, @Param("occupancyStatus") String occupancyStatus,
            @Param("sortBy") String sortBy, @Param("sortDirection") String sortDirection,
            @Param("offset") int offset, @Param("limit") int limit, @Param("scope") CommunityDataScope scope);
    int count(@Param("communityId") Long communityId, @Param("buildingId") Long buildingId,
            @Param("buildingUnitId") Long buildingUnitId, @Param("layoutTemplateId") Long layoutTemplateId,
            @Param("keyword") String keyword, @Param("status") String status,
            @Param("occupancyStatus") String occupancyStatus, @Param("scope") CommunityDataScope scope);
    PropertyUnitResponse findById(@Param("propertyUnitId") Long propertyUnitId, @Param("scope") CommunityDataScope scope);
    int insert(@Param("entity") PropertyUnitEntity entity,
            @Param("effectiveStatus") String effectiveStatus,
            @Param("effectiveOccupancyStatus") String effectiveOccupancyStatus);
    int update(@Param("propertyUnitId") Long propertyUnitId, @Param("entity") PropertyUnitEntity entity,
            @Param("effectiveStatus") String effectiveStatus,
            @Param("effectiveOccupancyStatus") String effectiveOccupancyStatus);
    Long lastInsertId();
}
