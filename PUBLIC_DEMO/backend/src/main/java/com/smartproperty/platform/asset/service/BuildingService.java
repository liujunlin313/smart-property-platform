package com.smartproperty.platform.asset.service;

import com.smartproperty.platform.asset.dto.request.BuildingCreateRequest;
import com.smartproperty.platform.asset.dto.request.BuildingUpdateRequest;
import com.smartproperty.platform.asset.dto.response.BuildingResponse;
import com.smartproperty.platform.asset.mapper.BuildingMapper;
import com.smartproperty.platform.asset.model.BuildingEntity;
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
public class BuildingService {
    private final BuildingMapper mapper;
    private final CommunityService communities;
    private final AssetAuditService audit;
    private final AuthorizationService authorizationService;
    private final DataScopeService dataScopeService;

    public BuildingService(BuildingMapper mapper, CommunityService communities, AssetAuditService audit, AuthorizationService authorizationService, DataScopeService dataScopeService) {
        this.mapper=mapper; this.communities=communities; this.audit=audit; this.authorizationService=authorizationService; this.dataScopeService=dataScopeService;
    }
    public List<BuildingResponse> findAll(Long communityId,String status){authorizationService.requirePermission(PermissionCodes.PROPERTY_VIEW);if(communityId!=null)dataScopeService.requireCommunity(communityId);return mapper.findAll(communityId,trimToNull(status),dataScopeService.currentCommunityScope());}
    public BuildingResponse get(Long id){authorizationService.requirePermission(PermissionCodes.PROPERTY_VIEW);dataScopeService.requireBuilding(id);var value=mapper.findById(id,dataScopeService.currentCommunityScope());if(value==null)throw notFound("BUILDING_NOT_FOUND","楼栋不存在");return value;}
    @Transactional public BuildingResponse create(BuildingCreateRequest request,Long accountId){
        communities.get(request.communityId()); mapper.insert(entity(request)); Long id=mapper.lastInsertId();
        var created=get(id); audit.record("BUILDING",id,"CREATE",null,created,request.changeReason(),accountId); return created;
    }
    @Transactional public BuildingResponse update(Long id,BuildingUpdateRequest request,Long accountId){
        var before=get(id); if(before.locationVerifiedAt()!=null && locationChanged(before,request))
            throw new BusinessException(HttpStatus.CONFLICT,"VERIFIED_LOCATION_LOCKED","该楼栋位置已经核验，请通过后续的位置纠错流程修改名称或坐标");
        if(mapper.update(id,entity(request))!=1)throw conflict(); var after=get(id);
        audit.record("BUILDING",id,"UPDATE",before,after,request.changeReason(),accountId); return after;
    }
    private BuildingEntity entity(BuildingCreateRequest r){return new BuildingEntity(r.communityId(),r.buildingNo(),r.buildingName(),r.totalFloors(),r.longitude(),r.latitude(),r.status(),null);}
    private BuildingEntity entity(BuildingUpdateRequest r){return new BuildingEntity(r.communityId(),r.buildingNo(),r.buildingName(),r.totalFloors(),r.longitude(),r.latitude(),r.status(),r.version());}
    private boolean locationChanged(BuildingResponse before,BuildingUpdateRequest request){return !Objects.equals(before.buildingName(),request.buildingName())||decimalChanged(before.longitude(),request.longitude())||decimalChanged(before.latitude(),request.latitude());}
    private boolean decimalChanged(BigDecimal before,BigDecimal after){if(before==null||after==null)return before!=after;return before.compareTo(after)!=0;}
}
