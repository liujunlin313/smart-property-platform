package com.smartproperty.platform.property.controller;

import com.smartproperty.platform.common.api.PageResult;
import com.smartproperty.platform.property.query.PropertyQuery;
import com.smartproperty.platform.property.service.PropertyService;
import com.smartproperty.platform.property.vo.PropertyDetailVO;
import com.smartproperty.platform.property.vo.PropertyListVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/properties")
public class PropertyController {
    private final PropertyService propertyService;
    public PropertyController(PropertyService propertyService) { this.propertyService = propertyService; }
    @GetMapping public PageResult<PropertyListVO> pageProperties(PropertyQuery query) { return propertyService.pageProperties(query); }
    @GetMapping("/{propertyUnitId}") public PropertyDetailVO getPropertyDetail(@PathVariable Long propertyUnitId) { return propertyService.getPropertyDetail(propertyUnitId); }
}
