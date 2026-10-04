package com.smartproperty.platform.dashboard.controller;
import com.smartproperty.platform.dashboard.service.DashboardService;
import com.smartproperty.platform.dashboard.vo.*;
import com.smartproperty.platform.statistics.vo.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/admin/dashboard")
public class DashboardController {
    private final DashboardService dashboardService; public DashboardController(DashboardService dashboardService) { this.dashboardService = dashboardService; }
    @GetMapping("/overview") public DashboardOverviewVO getOverview(@RequestParam Integer year) { return dashboardService.getOverview(year); }
    @GetMapping("/monthly-collection") public List<MonthlyCollectionVO> getMonthlyCollection(@RequestParam Integer year) { return dashboardService.getMonthlyCollection(year); }
    @GetMapping("/fee-composition") public List<FeeCompositionVO> getFeeComposition(@RequestParam Integer year) { return dashboardService.getFeeComposition(year); }
    @GetMapping("/bill-status") public List<BillStatusDistributionVO> getBillStatus(@RequestParam Integer year) { return dashboardService.getBillStatus(year); }
    @GetMapping("/occupancy") public List<OccupancyOverviewVO> getOccupancy() { return dashboardService.getOccupancy(); }
    @GetMapping("/overdue-top") public List<OverdueBillVO> getOverdueTop(@RequestParam(defaultValue = "10") int limit) { return dashboardService.getOverdueTop(limit); }
    @GetMapping("/residence") public ResidenceDashboardVO getResidenceDashboard() { return dashboardService.getResidenceDashboard(); }
    @GetMapping("/rentals") public RentalDashboardVO getRentalDashboard(@RequestParam Integer year) { return dashboardService.getRentalDashboard(year); }
}
