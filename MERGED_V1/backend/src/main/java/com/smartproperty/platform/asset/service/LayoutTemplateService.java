package com.smartproperty.platform.asset.service;
import com.smartproperty.platform.asset.dto.response.LayoutTemplateResponse;
import com.smartproperty.platform.asset.mapper.LayoutTemplateMapper;
import com.smartproperty.platform.access.AuthorizationService;
import com.smartproperty.platform.access.PermissionCodes;
import org.springframework.stereotype.Service;
import java.util.List;
@Service public class LayoutTemplateService {private final LayoutTemplateMapper mapper;private final AuthorizationService authorizationService;public LayoutTemplateService(LayoutTemplateMapper mapper,AuthorizationService authorizationService){this.mapper=mapper;this.authorizationService=authorizationService;}public List<LayoutTemplateResponse> findAll(){authorizationService.requirePermission(PermissionCodes.PROPERTY_VIEW);return mapper.findAll();}}
