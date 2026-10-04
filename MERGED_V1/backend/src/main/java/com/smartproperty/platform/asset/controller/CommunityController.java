package com.smartproperty.platform.asset.controller;

import com.smartproperty.platform.asset.dto.request.CommunityCreateRequest;
import com.smartproperty.platform.asset.dto.request.CommunityUpdateRequest;
import com.smartproperty.platform.asset.dto.response.CommunityResponse;
import com.smartproperty.platform.asset.service.CommunityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.smartproperty.platform.common.web.AuthenticatedRequest.accountId;

@RestController
@RequestMapping("/api/v1/communities")
public class CommunityController {
    private final CommunityService service;

    public CommunityController(CommunityService service) {
        this.service = service;
    }

    @GetMapping
    public List<CommunityResponse> findAll(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String status) {
        return service.findAll(keyword, status);
    }

    @GetMapping("/{id}")
    public CommunityResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommunityResponse create(@Valid @RequestBody CommunityCreateRequest body,
                                    @AuthenticationPrincipal Jwt jwt,
                                    HttpServletRequest request) {
        return service.create(body, accountId(jwt, request));
    }

    @PutMapping("/{id}")
    public CommunityResponse update(@PathVariable Long id,
                                    @Valid @RequestBody CommunityUpdateRequest body,
                                    @AuthenticationPrincipal Jwt jwt,
                                    HttpServletRequest request) {
        return service.update(id, body, accountId(jwt, request));
    }
}
