package com.smartproperty.platform.asset.mapper;

import com.smartproperty.platform.asset.dto.response.AssetDashboardResponse;
import com.smartproperty.platform.access.CommunityDataScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AssetDashboardMapper {
    AssetDashboardResponse summary(@Param("scope") CommunityDataScope scope);
}
