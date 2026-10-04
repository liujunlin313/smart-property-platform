package com.smartproperty.platform.asset.controller;

import com.smartproperty.platform.asset.dto.response.LayoutTemplateResponse;
import com.smartproperty.platform.asset.service.LayoutTemplateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/layout-templates")
public class LayoutTemplateController {
    private final LayoutTemplateService service;

    public LayoutTemplateController(LayoutTemplateService service) { this.service = service; }

    @GetMapping
    public List<LayoutTemplateResponse> findAll() { return service.findAll(); }
}
