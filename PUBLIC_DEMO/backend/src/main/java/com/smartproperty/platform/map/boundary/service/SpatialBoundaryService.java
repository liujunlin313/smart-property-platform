package com.smartproperty.platform.map.boundary.service;

import com.smartproperty.platform.map.boundary.dto.BoundarySaveRequest;
import com.smartproperty.platform.map.boundary.vo.SpatialBoundaryVO;
import com.smartproperty.platform.map.boundary.vo.SpatialSearchResultVO;

import java.util.List;

public interface SpatialBoundaryService {
    List<SpatialSearchResultVO> search(String keyword);
    SpatialBoundaryVO getBoundary(String subjectType, Long subjectId);
    SpatialBoundaryVO saveBoundary(String subjectType, Long subjectId, BoundarySaveRequest request);
}
