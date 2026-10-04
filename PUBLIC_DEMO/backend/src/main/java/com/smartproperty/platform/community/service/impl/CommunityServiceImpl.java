package com.smartproperty.platform.community.service.impl;

import com.smartproperty.platform.access.AuthorizationService;
import com.smartproperty.platform.access.DataScopeService;
import com.smartproperty.platform.access.PermissionCodes;
import com.smartproperty.platform.community.mapper.AdminCommunityMapper;
import com.smartproperty.platform.community.service.CommunityService;
import com.smartproperty.platform.community.vo.CommunityListVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommunityServiceImpl implements CommunityService {
    private final AdminCommunityMapper communityMapper;
    private final AuthorizationService authorizationService;
    private final DataScopeService dataScopeService;

    public CommunityServiceImpl(AdminCommunityMapper communityMapper, AuthorizationService authorizationService, DataScopeService dataScopeService) {
        this.communityMapper = communityMapper;
        this.authorizationService = authorizationService;
        this.dataScopeService = dataScopeService;
    }

    @Override
    public List<CommunityListVO> listCommunities() {
        authorizationService.requirePermission(PermissionCodes.PROPERTY_VIEW);
        return communityMapper.selectActiveCommunities(dataScopeService.currentCommunityScope());
    }
}
