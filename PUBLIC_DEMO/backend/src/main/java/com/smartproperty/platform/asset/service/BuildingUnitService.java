package com.smartproperty.platform.asset.service;

import com.smartproperty.platform.asset.dto.request.BuildingUnitCreateRequest;
import com.smartproperty.platform.asset.dto.request.BuildingUnitUpdateRequest;
import com.smartproperty.platform.asset.dto.response.BuildingUnitResponse;
import com.smartproperty.platform.asset.mapper.BuildingUnitMapper;
import com.smartproperty.platform.asset.model.BuildingUnitEntity;
import com.smartproperty.platform.common.audit.AssetAuditService;
import com.smartproperty.platform.access.AuthorizationService;
import com.smartproperty.platform.access.DataScopeService;
import com.smartproperty.platform.access.PermissionCodes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static com.smartproperty.platform.common.audit.AssetServiceSupport.*;

@Service
public class BuildingUnitService {
    private final BuildingUnitMapper mapper; private final BuildingService buildings; private final AssetAuditService audit; private final AuthorizationService authorizationService; private final DataScopeService dataScopeService;
    public BuildingUnitService(BuildingUnitMapper mapper,BuildingService buildings,AssetAuditService audit,AuthorizationService authorizationService,DataScopeService dataScopeService){this.mapper=mapper;this.buildings=buildings;this.audit=audit;this.authorizationService=authorizationService;this.dataScopeService=dataScopeService;}
    public List<BuildingUnitResponse> findAll(Long buildingId,String status){authorizationService.requirePermission(PermissionCodes.PROPERTY_VIEW);if(buildingId!=null)dataScopeService.requireBuilding(buildingId);return mapper.findAll(buildingId,trimToNull(status),dataScopeService.currentCommunityScope());}
    public BuildingUnitResponse get(Long id){authorizationService.requirePermission(PermissionCodes.PROPERTY_VIEW);var value=mapper.findById(id,dataScopeService.currentCommunityScope());if(value==null)throw notFound("BUILDING_UNIT_NOT_FOUND","楼栋单元不存在");dataScopeService.requireBuilding(value.buildingId());return value;}
    @Transactional public BuildingUnitResponse create(BuildingUnitCreateRequest request,Long accountId){
        buildings.get(request.buildingId());mapper.insert(entity(request));Long id=mapper.lastInsertId();var created=get(id);
        audit.record("BUILDING_UNIT",id,"CREATE",null,created,request.changeReason(),accountId);return created;}
    @Transactional public BuildingUnitResponse update(Long id,BuildingUnitUpdateRequest request,Long accountId){
        var before=get(id);if(mapper.update(id,entity(request))!=1)throw conflict();var after=get(id);
        audit.record("BUILDING_UNIT",id,"UPDATE",before,after,request.changeReason(),accountId);return after;}
    private BuildingUnitEntity entity(BuildingUnitCreateRequest r){return new BuildingUnitEntity(r.buildingId(),r.unitNo(),r.unitName(),r.status(),null);}
    private BuildingUnitEntity entity(BuildingUnitUpdateRequest r){return new BuildingUnitEntity(r.buildingId(),r.unitNo(),r.unitName(),r.status(),r.version());}
}
