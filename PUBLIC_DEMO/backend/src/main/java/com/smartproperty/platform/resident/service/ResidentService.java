package com.smartproperty.platform.resident.service;

import com.smartproperty.platform.common.api.PageResult;
import com.smartproperty.platform.resident.query.ResidentQuery;
import com.smartproperty.platform.resident.vo.ResidentDetailVO;
import com.smartproperty.platform.resident.vo.ResidentListVO;
import com.smartproperty.platform.resident.vo.ResidentSummaryVO;

public interface ResidentService {
    PageResult<ResidentListVO> pageResidents(ResidentQuery query);
    ResidentDetailVO getResidentDetail(Long residenceId);
    ResidentSummaryVO getSummary();
}
