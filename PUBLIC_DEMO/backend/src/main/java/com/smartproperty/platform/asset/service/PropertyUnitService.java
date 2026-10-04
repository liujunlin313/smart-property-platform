package com.smartproperty.platform.asset.service;

import com.smartproperty.platform.asset.dto.request.PropertyUnitCreateRequest;
import com.smartproperty.platform.asset.dto.request.PropertyUnitUpdateRequest;
import com.smartproperty.platform.asset.dto.response.PropertyUnitResponse;
import com.smartproperty.platform.asset.mapper.PropertyUnitMapper;
import com.smartproperty.platform.asset.model.PropertyUnitEntity;
import com.smartproperty.platform.common.api.PagedResponse;
import com.smartproperty.platform.common.audit.AssetAuditService;
import com.smartproperty.platform.access.AuthorizationService;
import com.smartproperty.platform.access.DataScopeService;
import com.smartproperty.platform.access.PermissionCodes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Set;
import static com.smartproperty.platform.common.audit.AssetServiceSupport.*;

@Service
public class PropertyUnitService {
    private static final Set<String> SORTS=Set.of("createdAt","updatedAt","floorNo","grossArea");
    private final PropertyUnitMapper mapper;private final BuildingUnitService units;private final AssetAuditService audit;private final AuthorizationService authorizationService;private final DataScopeService dataScopeService;
    public PropertyUnitService(PropertyUnitMapper mapper,BuildingUnitService units,AssetAuditService audit,AuthorizationService authorizationService,DataScopeService dataScopeService){this.mapper=mapper;this.units=units;this.audit=audit;this.authorizationService=authorizationService;this.dataScopeService=dataScopeService;}
    public PagedResponse<PropertyUnitResponse> findAll(Long communityId,Long buildingId,Long buildingUnitId,Long layoutTemplateId,String keyword,String status,String occupancyStatus,int page,int pageSize,String sortBy,String sortDirection){
        authorizationService.requirePermission(PermissionCodes.PROPERTY_VIEW);if(communityId!=null)dataScopeService.requireCommunity(communityId);if(buildingId!=null)dataScopeService.requireBuilding(buildingId);if(buildingUnitId!=null)units.get(buildingUnitId);if(page<1)throw invalid("页码必须从 1 开始");if(pageSize<1||pageSize>100)throw invalid("每页数量必须在 1 到 100 之间");
        String safeSort=SORTS.contains(sortBy)?sortBy:"createdAt",direction="asc".equalsIgnoreCase(sortDirection)?"asc":"desc";
        String safeKeyword=trimToNull(keyword),safeStatus=trimToNull(status),safeOccupancy=trimToNull(occupancyStatus);
        var scope=dataScopeService.currentCommunityScope();int total=mapper.count(communityId,buildingId,buildingUnitId,layoutTemplateId,safeKeyword,safeStatus,safeOccupancy,scope);
        var rows=mapper.findAll(communityId,buildingId,buildingUnitId,layoutTemplateId,safeKeyword,safeStatus,safeOccupancy,safeSort,direction,(page-1)*pageSize,pageSize,scope);
        return PagedResponse.of(rows,page,pageSize,total);
    }
    public PropertyUnitResponse get(Long id){authorizationService.requirePermission(PermissionCodes.PROPERTY_VIEW);dataScopeService.requireProperty(id);var value=mapper.findById(id,dataScopeService.currentCommunityScope());if(value==null)throw notFound("PROPERTY_NOT_FOUND","房产不存在");return value;}
    @Transactional public PropertyUnitResponse create(PropertyUnitCreateRequest request,Long accountId){
        units.get(request.buildingUnitId());validateArea(request.grossArea(),request.usableArea());
        String status=effectiveStatus(request.status(),null,request.grossArea());String occupancy=request.occupancyStatus()==null?"UNKNOWN":request.occupancyStatus();
        mapper.insert(entity(request),status,occupancy);Long id=mapper.lastInsertId();var created=get(id);
        audit.record("PROPERTY_UNIT",id,"CREATE",null,created,request.changeReason(),accountId);return created;}
    @Transactional public PropertyUnitResponse update(Long id,PropertyUnitUpdateRequest request,Long accountId){
        var before=get(id);validateArea(request.grossArea(),request.usableArea());String status=effectiveStatus(request.status(),before.status(),request.grossArea());
        String occupancy=request.occupancyStatus()==null?before.occupancyStatus():request.occupancyStatus();
        if(mapper.update(id,entity(request),status,occupancy)!=1)throw conflict();var after=get(id);
        audit.record("PROPERTY_UNIT",id,"UPDATE",before,after,request.changeReason(),accountId);return after;}
    private void validateArea(BigDecimal gross,BigDecimal usable){if(usable!=null&&gross==null)throw invalid("填写套内面积前必须先填写建筑面积");if(usable!=null&&usable.compareTo(gross)>0)throw invalid("套内面积不能大于建筑面积");}
    private String effectiveStatus(String requested,String previous,BigDecimal gross){if(gross==null)return "DRAFT";if(requested!=null)return requested;return previous==null?"DRAFT":previous;}
    private PropertyUnitEntity entity(PropertyUnitCreateRequest r){return new PropertyUnitEntity(r.buildingUnitId(),r.layoutTemplateId(),r.propertyNo(),r.floorNo(),r.grossArea(),r.usableArea(),r.propertyUse(),r.status(),r.occupancyStatus(),null);}
    private PropertyUnitEntity entity(PropertyUnitUpdateRequest r){return new PropertyUnitEntity(r.buildingUnitId(),r.layoutTemplateId(),r.propertyNo(),r.floorNo(),r.grossArea(),r.usableArea(),r.propertyUse(),r.status(),r.occupancyStatus(),r.version());}
}
