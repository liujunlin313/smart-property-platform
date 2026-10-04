package com.smartproperty.platform.asset.controller;

import com.smartproperty.platform.asset.dto.request.BuildingUnitCreateRequest;
import com.smartproperty.platform.asset.dto.request.BuildingUnitUpdateRequest;
import com.smartproperty.platform.asset.dto.response.BuildingUnitResponse;
import com.smartproperty.platform.asset.service.BuildingUnitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.smartproperty.platform.common.web.AuthenticatedRequest.accountId;

@RestController
@RequestMapping("/api/v1/building-units")
public class BuildingUnitController {
    private final BuildingUnitService service;

    public BuildingUnitController(BuildingUnitService service) { this.service = service; }

    @GetMapping
    public List<BuildingUnitResponse> findAll(@RequestParam(required = false) Long buildingId,
                                              @RequestParam(required = false) String status) {
        return service.findAll(buildingId, status);
    }

    @GetMapping("/{id}")
    public BuildingUnitResponse get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BuildingUnitResponse create(@Valid @RequestBody BuildingUnitCreateRequest body,
                                       @AuthenticationPrincipal Jwt jwt,
                                       HttpServletRequest request) {
        return service.create(body, accountId(jwt, request));
    }

    @PutMapping("/{id}")
    public BuildingUnitResponse update(@PathVariable Long id,
                                       @Valid @RequestBody BuildingUnitUpdateRequest body,
                                       @AuthenticationPrincipal Jwt jwt,
                                       HttpServletRequest request) {
        return service.update(id, body, accountId(jwt, request));
    }
}
