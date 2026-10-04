package com.smartproperty.platform.dashboard.mapper;
import com.smartproperty.platform.dashboard.vo.*;
import com.smartproperty.platform.access.CommunityDataScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
@Mapper
public interface DashboardMapper {
    DashboardOverviewVO selectOverview(@Param("year") Integer year, @Param("scope") CommunityDataScope scope);
    List<OverdueBillVO> selectOverdueTop(@Param("limit") int limit, @Param("scope") CommunityDataScope scope);
    ResidenceDashboardRow selectResidenceDashboard(@Param("scope") CommunityDataScope scope);
    List<DashboardDistributionVO> selectResidenceRelationDistribution(@Param("scope") CommunityDataScope scope);
    List<DashboardDistributionVO> selectResidentCommunityDistribution(@Param("scope") CommunityDataScope scope);
    RentalDashboardRow selectRentalDashboard(@Param("year") Integer year, @Param("scope") CommunityDataScope scope);
    List<DashboardDistributionVO> selectRentalCommunityDistribution(@Param("scope") CommunityDataScope scope);
    List<DashboardDistributionVO> selectRentalModeDistribution(@Param("scope") CommunityDataScope scope);
    List<DashboardDistributionVO> selectRentalAgencyDistribution(@Param("scope") CommunityDataScope scope);
    List<DashboardDistributionVO> selectRentalStatusDistribution(@Param("scope") CommunityDataScope scope);
    record ResidenceDashboardRow(long activeResidentCount, long responsibleCount, long ownerOccupiedCount, long tenantOccupiedCount, long availableForRentCount, long vacantCount, java.math.BigDecimal occupancyRatePercent) { }
    record RentalDashboardRow(long currentPublishedCount, long rentedInPeriodCount, long selfCount, long agencyCount, java.math.BigDecimal averageMonthlyRent, java.math.BigDecimal averageListingDays) { }
}
