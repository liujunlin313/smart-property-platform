package com.smartproperty.platform.statistics.vo;
import java.math.BigDecimal;
public record MonthlyCollectionVO(Integer year, Integer month, Long billCount, BigDecimal receivableAmount,
                                  BigDecimal receivedAmount, BigDecimal outstandingAmount, BigDecimal collectionRatePercent) {}
