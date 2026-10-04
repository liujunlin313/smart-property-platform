package com.smartproperty.platform.statistics.vo;
import java.math.BigDecimal;
public record AnnualCollectionSummaryVO(Integer year, Long billCount, BigDecimal receivableAmount,
                                        BigDecimal receivedAmount, BigDecimal outstandingAmount,
                                        BigDecimal collectionRatePercent) {}
