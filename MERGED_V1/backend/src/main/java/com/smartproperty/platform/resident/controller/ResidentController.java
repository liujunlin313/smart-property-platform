package com.smartproperty.platform.resident.controller;

import com.smartproperty.platform.common.api.PageResult;
import com.smartproperty.platform.resident.query.ResidentQuery;
import com.smartproperty.platform.resident.service.ResidentService;
import com.smartproperty.platform.resident.vo.ResidentDetailVO;
import com.smartproperty.platform.resident.vo.ResidentListVO;
import com.smartproperty.platform.resident.vo.ResidentSummaryVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/residents")
public class ResidentController {
    private final ResidentService residentService;
    public ResidentController(ResidentService residentService) { this.residentService = residentService; }
    @GetMapping public PageResult<ResidentListVO> pageResidents(ResidentQuery query) { return residentService.pageResidents(query); }
    @GetMapping("/summary") public ResidentSummaryVO getSummary() { return residentService.getSummary(); }
    @GetMapping("/{residenceId}") public ResidentDetailVO getResidentDetail(@PathVariable Long residenceId) { return residentService.getResidentDetail(residenceId); }
}
