package com.smartproperty.platform.billing.controller;
import com.smartproperty.platform.billing.query.BillQuery;
import com.smartproperty.platform.billing.service.BillService;
import com.smartproperty.platform.billing.vo.*;
import com.smartproperty.platform.common.api.PageResult;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/bills")
public class BillController {
    private final BillService billService; public BillController(BillService billService) { this.billService = billService; }
    @GetMapping public PageResult<BillListVO> pageBills(BillQuery query) { return billService.pageBills(query); }
    @GetMapping("/{billId}") public BillDetailVO getBillDetail(@PathVariable Long billId) { return billService.getBillDetail(billId); }
}
