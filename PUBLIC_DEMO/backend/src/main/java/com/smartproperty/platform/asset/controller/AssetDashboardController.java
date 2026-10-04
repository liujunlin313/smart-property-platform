package com.smartproperty.platform.asset.controller;

import com.smartproperty.platform.asset.dto.response.AssetDashboardResponse;
import com.smartproperty.platform.asset.service.AssetDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class AssetDashboardController {
    private final AssetDashboardService service;

    public AssetDashboardController(AssetDashboardService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public AssetDashboardResponse summary() {
        return service.summary();
    }
}
