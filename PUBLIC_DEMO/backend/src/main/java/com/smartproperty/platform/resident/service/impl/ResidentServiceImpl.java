package com.smartproperty.platform.resident.service.impl;

import com.smartproperty.platform.access.AuthorizationService;
import com.smartproperty.platform.access.DataScopeService;
import com.smartproperty.platform.access.PermissionCodes;
import com.smartproperty.platform.common.api.PageResult;
import com.smartproperty.platform.common.exception.BusinessException;
import com.smartproperty.platform.resident.mapper.ResidentMapper;
import com.smartproperty.platform.resident.query.ResidentQuery;
import com.smartproperty.platform.resident.service.ResidentService;
import com.smartproperty.platform.resident.vo.ResidentDetailVO;
import com.smartproperty.platform.resident.vo.ResidentListVO;
import com.smartproperty.platform.resident.vo.ResidentSummaryVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ResidentServiceImpl implements ResidentService {
    private final ResidentMapper residentMapper;
    private final AuthorizationService authorizationService;
    private final DataScopeService dataScopeService;
    public ResidentServiceImpl(ResidentMapper residentMapper, AuthorizationService authorizationService, DataScopeService dataScopeService) {
        this.residentMapper = residentMapper;
        this.authorizationService = authorizationService;
        this.dataScopeService = dataScopeService;
    }
    @Override public PageResult<ResidentListVO> pageResidents(ResidentQuery query) {
        authorizationService.requirePermission(PermissionCodes.RESIDENT_VIEW); normalize(query); dataScopeService.apply(query);
        long total = residentMapper.countResidents(query);
        return PageResult.of(residentMapper.selectResidentPage(query, (long) (query.getPage() - 1) * query.getPageSize(), query.getPageSize()), total, query.getPage(), query.getPageSize());
    }
    @Override public ResidentDetailVO getResidentDetail(Long residenceId) {
        authorizationService.requirePermission(PermissionCodes.RESIDENT_VIEW); dataScopeService.requireResidence(residenceId);
        ResidentDetailVO detail = residentMapper.selectResidentDetail(residenceId);
        if (detail == null) throw new BusinessException(HttpStatus.NOT_FOUND, "RESIDENCE_NOT_FOUND", "未找到当前居住记录");
        return detail;
    }
    @Override public ResidentSummaryVO getSummary() { authorizationService.requirePermission(PermissionCodes.RESIDENT_VIEW); return residentMapper.selectResidentSummary(dataScopeService.currentCommunityScope()); }
    private void normalize(ResidentQuery query) {
        if (query.getPage() == null || query.getPage() < 1) query.setPage(1);
        if (query.getPageSize() == null || query.getPageSize() < 1) query.setPageSize(20);
        if (query.getPageSize() > 100) query.setPageSize(100);
        if (query.getKeyword() != null) query.setKeyword(query.getKeyword().trim());
    }
}
