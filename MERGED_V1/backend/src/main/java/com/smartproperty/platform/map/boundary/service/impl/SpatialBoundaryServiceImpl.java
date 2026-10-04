package com.smartproperty.platform.map.boundary.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartproperty.platform.access.AuthorizationService;
import com.smartproperty.platform.access.DataScopeService;
import com.smartproperty.platform.access.PermissionCodes;
import com.smartproperty.platform.common.exception.BusinessException;
import com.smartproperty.platform.map.boundary.dto.BoundarySaveRequest;
import com.smartproperty.platform.map.boundary.mapper.SpatialBoundaryMapper;
import com.smartproperty.platform.map.boundary.service.SpatialBoundaryService;
import com.smartproperty.platform.map.boundary.vo.SpatialBoundaryVO;
import com.smartproperty.platform.map.boundary.vo.SpatialSearchResultVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class SpatialBoundaryServiceImpl implements SpatialBoundaryService {
    private final SpatialBoundaryMapper mapper;
    private final AuthorizationService authorizationService;
    private final DataScopeService dataScopeService;
    private final ObjectMapper objectMapper;

    public SpatialBoundaryServiceImpl(SpatialBoundaryMapper mapper, AuthorizationService authorizationService,
                                      DataScopeService dataScopeService, ObjectMapper objectMapper) {
        this.mapper = mapper; this.authorizationService = authorizationService;
        this.dataScopeService = dataScopeService; this.objectMapper = objectMapper;
    }
    @Override public List<SpatialSearchResultVO> search(String keyword) {
        authorizationService.requirePermission(PermissionCodes.MAP_VIEW);
        if (keyword == null || keyword.isBlank()) return List.of();
        return mapper.searchInternalObjects(keyword.trim(), dataScopeService.currentCommunityScope());
    }
    @Override public SpatialBoundaryVO getBoundary(String subjectType, Long subjectId) {
        authorizationService.requirePermission(PermissionCodes.MAP_VIEW);
        String type = normalizeType(subjectType); verifySubjectScope(type, subjectId);
        return mapper.selectBoundary(type, subjectId);
    }
    @Override public SpatialBoundaryVO saveBoundary(String subjectType, Long subjectId, BoundarySaveRequest request) {
        var actor = authorizationService.requirePermission(PermissionCodes.MAP_BOUNDARY_EDIT);
        String type = normalizeType(subjectType); verifySubjectScope(type, subjectId); validatePolygon(request.geoJson());
        int changed = mapper.upsertBoundary(type, subjectId, request.geoJson(), actor.accountId(), request.version());
        if (changed == 0) throw new BusinessException(HttpStatus.CONFLICT, "BOUNDARY_VERSION_CONFLICT", "边界已被其他操作更新，请刷新后再保存");
        return mapper.selectBoundary(type, subjectId);
    }
    private void verifySubjectScope(String type, Long subjectId) {
        if (subjectId == null || subjectId < 1) throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_SPATIAL_SUBJECT", "空间对象不能为空");
        if ("COMMUNITY".equals(type)) dataScopeService.requireCommunity(subjectId); else dataScopeService.requireBuilding(subjectId);
    }
    private String normalizeType(String subjectType) {
        String type = subjectType == null ? "" : subjectType.trim().toUpperCase(Locale.ROOT);
        if (!"COMMUNITY".equals(type) && !"BUILDING".equals(type)) throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_BOUNDARY_SUBJECT", "边界对象只能是小区或楼栋");
        return type;
    }
    private void validatePolygon(String geoJson) {
        try {
            JsonNode root = objectMapper.readTree(geoJson);
            if (!"Polygon".equals(root.path("type").asText()) || !root.path("coordinates").isArray() || root.path("coordinates").isEmpty()) throw new IllegalArgumentException();
            JsonNode ring = root.path("coordinates").get(0);
            if (!ring.isArray() || ring.size() < 4 || !ring.get(0).equals(ring.get(ring.size() - 1))) throw new IllegalArgumentException();
            for (JsonNode point : ring) if (!point.isArray() || point.size() < 2 || !point.get(0).isNumber() || !point.get(1).isNumber()) throw new IllegalArgumentException();
        } catch (Exception ignored) { throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_GEOJSON_POLYGON", "边界必须是闭合的 GeoJSON Polygon"); }
    }
}
