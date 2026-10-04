package com.smartproperty.platform.asset.mapper;

import com.smartproperty.platform.asset.dto.response.CommunityResponse;
import com.smartproperty.platform.asset.model.CommunityEntity;
import com.smartproperty.platform.access.CommunityDataScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface CommunityMapper {
    List<CommunityResponse> findAll(@Param("keyword") String keyword, @Param("status") String status, @Param("scope") CommunityDataScope scope);
    CommunityResponse findById(@Param("communityId") Long communityId, @Param("scope") CommunityDataScope scope);
    int insert(@Param("entity") CommunityEntity entity);
    int update(@Param("communityId") Long communityId, @Param("entity") CommunityEntity entity);
    Long lastInsertId();
}
