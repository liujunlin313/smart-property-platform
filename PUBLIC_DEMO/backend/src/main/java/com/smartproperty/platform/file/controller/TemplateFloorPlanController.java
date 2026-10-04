package com.smartproperty.platform.file.controller;

import com.smartproperty.platform.file.service.TemplateFloorPlanImportService;
import com.smartproperty.platform.file.service.FloorPlanVariantImportService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/floor-plan-templates")
public class TemplateFloorPlanController {
    private final TemplateFloorPlanImportService service;
    private final FloorPlanVariantImportService variantService;

    public TemplateFloorPlanController(TemplateFloorPlanImportService service,
                                       FloorPlanVariantImportService variantService) {
        this.service = service;
        this.variantService = variantService;
    }
    @PostMapping("/import-standard")
    public TemplateFloorPlanImportService.ImportResult importStandardPlans() { return service.importStandardPlans(); }

    @PostMapping("/variants/import-standard")
    public FloorPlanVariantImportService.ImportResult importStandardVariants() {
        return variantService.importStandardVariants();
    }
}
