package com.smartproperty.platform.statistics.service;
import com.smartproperty.platform.statistics.vo.*;
import java.util.List;
public interface StatisticsService {
    List<MonthlyCollectionVO> getMonthlyCollectionStatistics(Integer year);
    AnnualCollectionVO getAnnualCollectionStatistics(Integer year);
    List<CommunityMonthlyVO> getMonthlyCommunities(Integer year, Integer month);
    MonthlyDrilldownVO getMonthlyDrilldown(Integer year, Integer month, Long communityId);
}
