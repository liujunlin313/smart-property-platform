package com.smartproperty.platform.dashboard.service;
import com.smartproperty.platform.dashboard.vo.*;
import com.smartproperty.platform.statistics.vo.*;
import java.util.List;
public interface DashboardService {
    DashboardOverviewVO getOverview(Integer year); List<MonthlyCollectionVO> getMonthlyCollection(Integer year);
    List<FeeCompositionVO> getFeeComposition(Integer year); List<BillStatusDistributionVO> getBillStatus(Integer year);
    List<OccupancyOverviewVO> getOccupancy(); List<OverdueBillVO> getOverdueTop(int limit);
    ResidenceDashboardVO getResidenceDashboard(); RentalDashboardVO getRentalDashboard(Integer year);
}
