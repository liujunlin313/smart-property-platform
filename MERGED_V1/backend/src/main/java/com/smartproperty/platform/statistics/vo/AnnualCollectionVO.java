package com.smartproperty.platform.statistics.vo;
import java.math.BigDecimal;
import java.util.List;
public record AnnualCollectionVO(Integer year, Long billCount, BigDecimal receivableAmount, BigDecimal receivedAmount,
                                 BigDecimal outstandingAmount, BigDecimal collectionRatePercent,
                                 List<QuarterlyCollectionVO> quarterlyStatistics, List<FeeCompositionVO> feeComposition) {}
