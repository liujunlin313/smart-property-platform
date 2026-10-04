package com.smartproperty.platform.statistics.vo;
import java.math.BigDecimal;
public record QuarterlyCollectionVO(Integer quarter, Long billCount, BigDecimal receivableAmount,
                                    BigDecimal receivedAmount, BigDecimal outstandingAmount, BigDecimal collectionRatePercent) {}
