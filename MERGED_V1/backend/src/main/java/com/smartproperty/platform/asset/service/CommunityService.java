package com.smartproperty.platform.asset.service;

import com.smartproperty.platform.asset.dto.request.CommunityCreateRequest;
import com.smartproperty.platform.asset.dto.request.CommunityUpdateRequest;
import com.smartproperty.platform.asset.dto.response.CommunityResponse;
import com.smartproperty.platform.asset.mapper.CommunityMapper;
import com.smartproperty.platform.asset.model.CommunityEntity;
import com.smartproperty.platform.common.exception.BusinessException;
import com.smartproperty.platform.common.audit.AssetAuditService;
import com.smartproperty.platform.access.AuthorizationService;
import com.smartproperty.platform.access.DataScopeService;
import com.smartproperty.platform.access.PermissionCodes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static com.smartproperty.platform.common.audit.AssetServiceSupport.*;

@Service
public class CommunityService {
    private final CommunityMapper mapper;
    private final AssetAuditService audit;
    private final AuthorizationService authorizationService;
    private final DataScopeService dataScopeService;

    public CommunityService(CommunityMapper mapper, AssetAuditService audit, AuthorizationService authorizationService, DataScopeService dataScopeService) {
        this.mapper = mapper;
        this.audit = audit;
        this.authorizationService = authorizationService;
        this.dataScopeService = dataScopeService;
    }

    public List<CommunityResponse> findAll(String keyword, String status) {
        authorizationService.requirePermission(PermissionCodes.PROPERTY_VIEW);
        return mapper.findAll(trimToNull(keyword), trimToNull(status), dataScopeService.currentCommunityScope());
    }

    public CommunityResponse get(Long id) {
        authorizationService.requirePermission(PermissionCodes.PROPERTY_VIEW);
        dataScopeService.requireCommunity(id);
        CommunityResponse response = mapper.findById(id, dataScopeService.currentCommunityScope());
        if (response == null) throw notFound("COMMUNITY_NOT_FOUND", "小区不存在");
        return response;
    }

    @Transactional
    public CommunityResponse create(CommunityCreateRequest request, Long accountId) {
        mapper.insert(entity(request));
        Long id = mapper.lastInsertId();
        CommunityResponse created = get(id);
        audit.record("COMMUNITY", id, "CREATE", null, created, request.changeReason(), accountId);
        return created;
    }

    @Transactional
    public CommunityResponse update(Long id, CommunityUpdateRequest request, Long accountId) {
        CommunityResponse before = get(id);
        if (before.locationVerifiedAt() != null && locationChanged(before, request)) {
            throw new BusinessException(HttpStatus.CONFLICT, "VERIFIED_LOCATION_LOCKED",
                    "该小区位置已经核验，请通过后续的位置纠错流程修改名称、地址或坐标");
        }
        if (mapper.update(id, entity(request)) != 1) throw conflict();
        CommunityResponse after = get(id);
        audit.record("COMMUNITY", id, "UPDATE", before, after, request.changeReason(), accountId);
        return after;
    }

    private CommunityEntity entity(CommunityCreateRequest request) {
        return new CommunityEntity(request.communityCode(), request.communityName(), request.addressLine(),
                request.longitude(), request.latitude(), request.completionYear(), request.status(), null);
    }

    private CommunityEntity entity(CommunityUpdateRequest request) {
        return new CommunityEntity(request.communityCode(), request.communityName(), request.addressLine(),
                request.longitude(), request.latitude(), request.completionYear(), request.status(), request.version());
    }

    private boolean locationChanged(CommunityResponse before, CommunityUpdateRequest request) {
        return !Objects.equals(before.communityName(), request.communityName())
                || !Objects.equals(before.addressLine(), request.addressLine())
                || decimalChanged(before.longitude(), request.longitude())
                || decimalChanged(before.latitude(), request.latitude());
    }

    private boolean decimalChanged(BigDecimal before, BigDecimal after) {
        if (before == null || after == null) return before != after;
        return before.compareTo(after) != 0;
    }
}
