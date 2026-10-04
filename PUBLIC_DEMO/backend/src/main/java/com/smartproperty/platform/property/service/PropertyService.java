package com.smartproperty.platform.property.service;

import com.smartproperty.platform.common.api.PageResult;
import com.smartproperty.platform.property.query.PropertyQuery;
import com.smartproperty.platform.property.vo.PropertyDetailVO;
import com.smartproperty.platform.property.vo.PropertyListVO;

public interface PropertyService {
    PageResult<PropertyListVO> pageProperties(PropertyQuery query);
    PropertyDetailVO getPropertyDetail(Long propertyUnitId);
}
