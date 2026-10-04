package com.smartproperty.platform.community.mapper;

import com.smartproperty.platform.access.CommunityDataScope;
import com.smartproperty.platform.community.vo.CommunityListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface AdminCommunityMapper {
    List<CommunityListVO> selectActiveCommunities(@Param("scope") CommunityDataScope scope);
}
