package com.smartproperty.platform.rental.vo;

import java.math.BigDecimal;
import java.util.List;

public record RentalSummaryVO(long currentPublishedCount, long rentedHistoryCount, long selfCount, long agencyCount,
                              long rentedInPeriodCount, BigDecimal averageMonthlyRent, BigDecimal averageListingDays,
                              List<NamedCountVO> communityDistribution, List<NamedCountVO> modeDistribution,
                              List<NamedCountVO> agencyDistribution, List<NamedCountVO> statusDistribution,
                              List<NamedCountVO> rentRangeDistribution, List<NamedCountVO> layoutDistribution) { }
