package com.smartproperty.platform.dashboard.service.impl;
import com.smartproperty.platform.access.AuthorizationService;
import com.smartproperty.platform.access.DataScopeService;
import com.smartproperty.platform.access.CommunityDataScope;
import com.smartproperty.platform.access.PermissionCodes;
import com.smartproperty.platform.dashboard.mapper.DashboardMapper;
import com.smartproperty.platform.dashboard.service.DashboardService;
import com.smartproperty.platform.dashboard.vo.*;
import com.smartproperty.platform.statistics.mapper.StatisticsMapper;
import com.smartproperty.platform.statistics.vo.*;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class DashboardServiceImpl implements DashboardService {
    private final DashboardMapper dashboardMapper; private final StatisticsMapper statisticsMapper; private final AuthorizationService authorizationService; private final DataScopeService dataScopeService;
    public DashboardServiceImpl(DashboardMapper dashboardMapper, StatisticsMapper statisticsMapper, AuthorizationService authorizationService, DataScopeService dataScopeService) { this.dashboardMapper = dashboardMapper; this.statisticsMapper = statisticsMapper; this.authorizationService = authorizationService; this.dataScopeService = dataScopeService; }
    private CommunityDataScope billingScope(){authorizationService.requirePermission(PermissionCodes.BILLING_VIEW);return dataScopeService.currentCommunityScope();}
    private CommunityDataScope propertyScope(){authorizationService.requirePermission(PermissionCodes.PROPERTY_VIEW);return dataScopeService.currentCommunityScope();}
    private CommunityDataScope residentScope(){authorizationService.requirePermission(PermissionCodes.RESIDENT_VIEW);return dataScopeService.currentCommunityScope();}
    public DashboardOverviewVO getOverview(Integer year) { var scope=billingScope(); return dashboardMapper.selectOverview(year,scope); }
    public List<MonthlyCollectionVO> getMonthlyCollection(Integer year) { var scope=billingScope(); return statisticsMapper.selectMonthlyCollection(year,scope); }
    public List<FeeCompositionVO> getFeeComposition(Integer year) { var scope=billingScope(); return statisticsMapper.selectFeeComposition(year,scope); }
    public List<BillStatusDistributionVO> getBillStatus(Integer year) { var scope=billingScope(); return statisticsMapper.selectBillStatusDistribution(year,scope); }
    public List<OccupancyOverviewVO> getOccupancy() { var scope=propertyScope(); return statisticsMapper.selectOccupancyOverview(scope); }
    public List<OverdueBillVO> getOverdueTop(int limit) { var scope=billingScope(); return dashboardMapper.selectOverdueTop(Math.min(Math.max(limit, 1), 50),scope); }
    public ResidenceDashboardVO getResidenceDashboard() { var scope=residentScope(); var row = dashboardMapper.selectResidenceDashboard(scope); return new ResidenceDashboardVO(row.activeResidentCount(), row.responsibleCount(), row.ownerOccupiedCount(), row.tenantOccupiedCount(), row.availableForRentCount(), row.vacantCount(), row.occupancyRatePercent(), statisticsMapper.selectOccupancyOverview(scope).stream().map(x -> new DashboardDistributionVO(x.occupancyStatus(), x.propertyCount())).toList(), dashboardMapper.selectResidenceRelationDistribution(scope), dashboardMapper.selectResidentCommunityDistribution(scope)); }
    public RentalDashboardVO getRentalDashboard(Integer year) { var scope=propertyScope(); var row = dashboardMapper.selectRentalDashboard(year,scope); return new RentalDashboardVO(row.currentPublishedCount(), row.rentedInPeriodCount(), row.selfCount(), row.agencyCount(), row.averageMonthlyRent(), row.averageListingDays(), dashboardMapper.selectRentalCommunityDistribution(scope), dashboardMapper.selectRentalModeDistribution(scope), dashboardMapper.selectRentalAgencyDistribution(scope), dashboardMapper.selectRentalStatusDistribution(scope)); }
}
