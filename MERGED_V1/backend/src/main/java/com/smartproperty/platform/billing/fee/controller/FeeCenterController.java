package com.smartproperty.platform.billing.fee.controller;
import com.smartproperty.platform.billing.fee.query.*;
import com.smartproperty.platform.billing.fee.service.FeeCenterService;
import com.smartproperty.platform.billing.fee.vo.*;
import com.smartproperty.platform.common.api.PageResult;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/admin/fees")
public class FeeCenterController {
    private final FeeCenterService feeCenterService;
    public FeeCenterController(FeeCenterService feeCenterService) { this.feeCenterService = feeCenterService; }
    @GetMapping("/types") public java.util.List<FeeTypeOptionVO> listActiveFeeTypes() { return feeCenterService.listActiveFeeTypes(); }
    @GetMapping("/standards") public PageResult<FeeStandardVO> pageStandards(FeeStandardQuery query) { return feeCenterService.pageFeeStandards(query); }
    @GetMapping("/payments") public PageResult<PaymentListVO> pagePayments(PaymentQuery query) { return feeCenterService.pagePayments(query); }
    @GetMapping("/overdue") public OverdueOverviewVO getOverdueOverview(OverdueQuery query) { return feeCenterService.getOverdueOverview(query); }
    @GetMapping("/overdue/list") public PageResult<OverdueBillVO> pageOverdueBills(OverdueQuery query) { return feeCenterService.pageOverdueBills(query); }
}
