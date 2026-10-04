package com.smartproperty.platform.asset.controller;

import com.smartproperty.platform.asset.dto.request.BuildingCreateRequest;
import com.smartproperty.platform.asset.dto.request.BuildingUpdateRequest;
import com.smartproperty.platform.asset.dto.response.BuildingResponse;
import com.smartproperty.platform.asset.service.BuildingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.smartproperty.platform.common.web.AuthenticatedRequest.accountId;

@RestController
@RequestMapping("/api/v1/buildings")
public class BuildingController {
    private final BuildingService service;

    public BuildingController(BuildingService service) { this.service = service; }

    @GetMapping
    public List<BuildingResponse> findAll(@RequestParam(required = false) Long communityId,
                                          @RequestParam(required = false) String status) {
        return service.findAll(communityId, status);
    }

    @GetMapping("/{id}")
    public BuildingResponse get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BuildingResponse create(@Valid @RequestBody BuildingCreateRequest body,
                                   @AuthenticationPrincipal Jwt jwt,
                                   HttpServletRequest request) {
        return service.create(body, accountId(jwt, request));
    }

    @PutMapping("/{id}")
    public BuildingResponse update(@PathVariable Long id,
                                   @Valid @RequestBody BuildingUpdateRequest body,
                                   @AuthenticationPrincipal Jwt jwt,
                                   HttpServletRequest request) {
        return service.update(id, body, accountId(jwt, request));
    }
}
