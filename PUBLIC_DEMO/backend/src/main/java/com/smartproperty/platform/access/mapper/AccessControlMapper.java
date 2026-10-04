package com.smartproperty.platform.access.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface AccessControlMapper {
    List<String> selectPermissionCodes(@Param("accountId") Long accountId);
    List<Long> selectActiveCommunityScopeIds(@Param("accountId") Long accountId);
    Long selectCommunityIdForProperty(@Param("propertyUnitId") Long propertyUnitId);
    Long selectCommunityIdForBill(@Param("billId") Long billId);
    Long selectCommunityIdForResidence(@Param("residenceId") Long residenceId);
    Long selectCommunityIdForBuilding(@Param("buildingId") Long buildingId);
    Long selectCommunityIdForPersonCurrentResidence(@Param("personId") Long personId);
    List<com.smartproperty.platform.access.vo.ScopedCommunityVO> selectScopedCommunities(@Param("global") boolean global, @Param("communityIds") List<Long> communityIds);
}
