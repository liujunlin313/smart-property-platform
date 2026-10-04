package com.smartproperty.platform.dashboard.vo;
import java.math.BigDecimal;
import java.util.List;
public record ResidenceDashboardVO(long activeResidentCount, long responsibleCount, long ownerOccupiedCount,
                                   long tenantOccupiedCount, long availableForRentCount, long vacantCount,
                                   BigDecimal occupancyRatePercent, List<DashboardDistributionVO> occupancyDistribution,
                                   List<DashboardDistributionVO> relationDistribution, List<DashboardDistributionVO> communityDistribution) { }
