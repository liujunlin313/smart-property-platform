package com.smartproperty.platform.statistics.controller;
import com.smartproperty.platform.statistics.service.StatisticsService;
import com.smartproperty.platform.statistics.vo.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/admin/statistics")
public class StatisticsController {
    private final StatisticsService statisticsService; public StatisticsController(StatisticsService statisticsService) { this.statisticsService = statisticsService; }
    @GetMapping("/monthly") public List<MonthlyCollectionVO> getMonthlyCollectionStatistics(@RequestParam Integer year) { return statisticsService.getMonthlyCollectionStatistics(year); }
    @GetMapping("/annual") public AnnualCollectionVO getAnnualCollectionStatistics(@RequestParam Integer year) { return statisticsService.getAnnualCollectionStatistics(year); }
    @GetMapping("/monthly/communities") public List<CommunityMonthlyVO> getMonthlyCommunities(@RequestParam Integer year, @RequestParam Integer month) { return statisticsService.getMonthlyCommunities(year, month); }
    @GetMapping("/monthly/drilldown") public MonthlyDrilldownVO getMonthlyDrilldown(@RequestParam Integer year, @RequestParam Integer month, @RequestParam(required = false) Long communityId) { return statisticsService.getMonthlyDrilldown(year, month, communityId); }
}
