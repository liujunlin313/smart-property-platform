package com.smartproperty.platform.map.boundary.mapper;

import com.smartproperty.platform.access.CommunityDataScope;
import com.smartproperty.platform.map.boundary.vo.SpatialBoundaryVO;
import com.smartproperty.platform.map.boundary.vo.SpatialSearchResultVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SpatialBoundaryMapper {
    List<SpatialSearchResultVO> searchInternalObjects(@Param("keyword") String keyword, @Param("scope") CommunityDataScope scope);
    SpatialBoundaryVO selectBoundary(@Param("subjectType") String subjectType, @Param("subjectId") Long subjectId);
    int upsertBoundary(@Param("subjectType") String subjectType, @Param("subjectId") Long subjectId,
                       @Param("geoJson") String geoJson, @Param("accountId") Long accountId,
                       @Param("expectedVersion") Long expectedVersion);
}
