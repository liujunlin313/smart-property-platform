package com.smartproperty.platform.asset.controller;

import com.smartproperty.platform.asset.dto.request.PropertyUnitCreateRequest;
import com.smartproperty.platform.asset.dto.request.PropertyUnitUpdateRequest;
import com.smartproperty.platform.asset.dto.response.PropertyUnitResponse;
import com.smartproperty.platform.asset.service.PropertyUnitService;
import com.smartproperty.platform.common.api.PagedResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import static com.smartproperty.platform.common.web.AuthenticatedRequest.accountId;

@RestController
@RequestMapping("/api/v1/properties")
public class PropertyUnitController {
    private final PropertyUnitService service;

    public PropertyUnitController(PropertyUnitService service) { this.service = service; }

    @GetMapping
    public PagedResponse<PropertyUnitResponse> findAll(
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Long buildingUnitId,
            @RequestParam(required = false) Long layoutTemplateId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String occupancyStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        return service.findAll(communityId, buildingId, buildingUnitId, layoutTemplateId,
                keyword, status, occupancyStatus, page, pageSize, sortBy, sortDirection);
    }

    @GetMapping("/{id}")
    public PropertyUnitResponse get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyUnitResponse create(@Valid @RequestBody PropertyUnitCreateRequest body,
                                       @AuthenticationPrincipal Jwt jwt,
                                       HttpServletRequest request) {
        return service.create(body, accountId(jwt, request));
    }

    @PutMapping("/{id}")
    public PropertyUnitResponse update(@PathVariable Long id,
                                       @Valid @RequestBody PropertyUnitUpdateRequest body,
                                       @AuthenticationPrincipal Jwt jwt,
                                       HttpServletRequest request) {
        return service.update(id, body, accountId(jwt, request));
    }
}
