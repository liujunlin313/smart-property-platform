package com.smartproperty.platform.property.service.impl;

import com.smartproperty.platform.access.AuthorizationService;
import com.smartproperty.platform.access.DataScopeService;
import com.smartproperty.platform.access.PermissionCodes;
import com.smartproperty.platform.common.api.PageResult;
import com.smartproperty.platform.common.exception.BusinessException;
import com.smartproperty.platform.property.mapper.PropertyMapper;
import com.smartproperty.platform.property.query.PropertyQuery;
import com.smartproperty.platform.property.service.PropertyService;
import com.smartproperty.platform.property.vo.PropertyDetailVO;
import com.smartproperty.platform.property.vo.PropertyDetailBaseVO;
import com.smartproperty.platform.property.vo.PropertyListVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PropertyServiceImpl implements PropertyService {
    private final PropertyMapper propertyMapper;
    private final AuthorizationService authorizationService;
    private final DataScopeService dataScopeService;

    public PropertyServiceImpl(PropertyMapper propertyMapper, AuthorizationService authorizationService, DataScopeService dataScopeService) {
        this.propertyMapper = propertyMapper;
        this.authorizationService = authorizationService;
        this.dataScopeService = dataScopeService;
    }

    @Override
    public PageResult<PropertyListVO> pageProperties(PropertyQuery query) {
        authorizationService.requirePermission(PermissionCodes.PROPERTY_VIEW);
        normalize(query);
        dataScopeService.apply(query);
        long total = propertyMapper.countProperties(query);
        long offset = (long) (query.getPage() - 1) * query.getPageSize();
        return PageResult.of(propertyMapper.selectPropertyPage(query, offset, query.getPageSize()), total,
                query.getPage(), query.getPageSize());
    }

    @Override
    public PropertyDetailVO getPropertyDetail(Long propertyUnitId) {
        authorizationService.requirePermission(PermissionCodes.PROPERTY_VIEW);
        dataScopeService.requireProperty(propertyUnitId);
        PropertyDetailBaseVO detail = propertyMapper.selectPropertyDetail(propertyUnitId);
        if (detail == null) throw new BusinessException(HttpStatus.NOT_FOUND, "PROPERTY_NOT_FOUND", "未找到指定房产");
        return new PropertyDetailVO(detail.propertyUnitId(), detail.propertyNo(), detail.communityName(), detail.buildingName(),
                detail.unitName(), detail.layoutType(), detail.grossArea(), detail.usableArea(), detail.propertyUse(),
                detail.occupancyStatus(), detail.status(), propertyMapper.selectCurrentOwners(propertyUnitId),
                propertyMapper.selectCurrentResidents(propertyUnitId), propertyMapper.selectActiveMeters(propertyUnitId));
    }

    private void normalize(PropertyQuery query) {
        if (query.getPage() == null || query.getPage() < 1) query.setPage(1);
        if (query.getPageSize() == null || query.getPageSize() < 1) query.setPageSize(20);
        if (query.getPageSize() > 100) query.setPageSize(100);
        if (query.getKeyword() != null) query.setKeyword(query.getKeyword().trim());
    }
}
