package com.smartproperty.platform.auth.provision.controller;

import com.smartproperty.platform.auth.provision.dto.DemoAccountProvisionReport;
import com.smartproperty.platform.auth.provision.service.DemoAccountProvisionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/accounts")
public class DemoAccountProvisionController {
    private final DemoAccountProvisionService service;
    public DemoAccountProvisionController(DemoAccountProvisionService service) { this.service = service; }
    @PostMapping("/provision-stage-g") public DemoAccountProvisionReport provisionStageG() { return service.provisionStageGDemoAccounts(); }
}
