package com.smartproperty.platform.statistics.vo;
import java.math.BigDecimal;
import java.util.List;
public record MonthlyDrilldownVO(Integer year, Integer month, Long communityId, String communityName, Long billCount,
                                 BigDecimal receivableAmount, BigDecimal receivedAmount, BigDecimal outstandingAmount,
                                 BigDecimal collectionRatePercent, List<FeeCompositionVO> feeComposition,
                                 List<BillStatusDistributionVO> billStatusDistribution) { }
