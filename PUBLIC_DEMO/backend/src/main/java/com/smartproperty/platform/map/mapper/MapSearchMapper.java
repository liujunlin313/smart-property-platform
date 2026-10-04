package com.smartproperty.platform.map.mapper;

import com.smartproperty.platform.access.CommunityDataScope;
import com.smartproperty.platform.map.model.ManagedCommunityLocationRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MapSearchMapper {
    ManagedCommunityLocationRow findVisibleCommunity(@Param("communityId") Long communityId,
                                                      @Param("scope") CommunityDataScope scope);
    List<ManagedCommunityLocationRow> findVisibleCommunityLocations(@Param("scope") CommunityDataScope scope);
}
