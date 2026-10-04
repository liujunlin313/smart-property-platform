package com.smartproperty.platform.dashboard.vo;
import java.math.BigDecimal;
import java.util.List;
public record RentalDashboardVO(long currentPublishedCount, long rentedInPeriodCount, long selfCount, long agencyCount,
                                BigDecimal averageMonthlyRent, BigDecimal averageListingDays,
                                List<DashboardDistributionVO> communityDistribution, List<DashboardDistributionVO> modeDistribution,
                                List<DashboardDistributionVO> agencyDistribution, List<DashboardDistributionVO> statusDistribution) { }
