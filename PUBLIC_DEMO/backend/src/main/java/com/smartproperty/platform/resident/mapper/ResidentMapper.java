package com.smartproperty.platform.resident.mapper;

import com.smartproperty.platform.resident.query.ResidentQuery;
import com.smartproperty.platform.resident.vo.ResidentDetailVO;
import com.smartproperty.platform.resident.vo.ResidentListVO;
import com.smartproperty.platform.resident.vo.ResidentSummaryVO;
import com.smartproperty.platform.access.CommunityDataScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ResidentMapper {
    long countResidents(@Param("query") ResidentQuery query);
    List<ResidentListVO> selectResidentPage(@Param("query") ResidentQuery query, @Param("offset") long offset, @Param("limit") int limit);
    ResidentDetailVO selectResidentDetail(@Param("residenceId") Long residenceId);
    ResidentSummaryVO selectResidentSummary(@Param("scope") CommunityDataScope scope);
}
