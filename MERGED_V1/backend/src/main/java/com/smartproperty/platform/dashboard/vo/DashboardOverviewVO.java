package com.smartproperty.platform.dashboard.vo;
import java.math.BigDecimal;
public record DashboardOverviewVO(Long propertyCount, Long occupiedPropertyCount, BigDecimal occupancyRatePercent,
                                  BigDecimal receivableAmount, BigDecimal receivedAmount, BigDecimal outstandingAmount,
                                  BigDecimal collectionRatePercent) {}
