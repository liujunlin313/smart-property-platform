package com.smartproperty.platform.asset.service;
import com.smartproperty.platform.asset.dto.response.AssetDashboardResponse;
import com.smartproperty.platform.asset.mapper.AssetDashboardMapper;
import com.smartproperty.platform.access.AuthorizationService;
import com.smartproperty.platform.access.DataScopeService;
import com.smartproperty.platform.access.PermissionCodes;
import org.springframework.stereotype.Service;
@Service public class AssetDashboardService {private final AssetDashboardMapper mapper;private final AuthorizationService authorizationService;private final DataScopeService dataScopeService;public AssetDashboardService(AssetDashboardMapper mapper,AuthorizationService authorizationService,DataScopeService dataScopeService){this.mapper=mapper;this.authorizationService=authorizationService;this.dataScopeService=dataScopeService;}public AssetDashboardResponse summary(){authorizationService.requirePermission(PermissionCodes.PROPERTY_VIEW);return mapper.summary(dataScopeService.currentCommunityScope());}}
