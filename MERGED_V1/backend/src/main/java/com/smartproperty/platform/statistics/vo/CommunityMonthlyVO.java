package com.smartproperty.platform.statistics.vo;
import java.math.BigDecimal;
public record CommunityMonthlyVO(Long communityId, String communityName, Long billCount, BigDecimal receivableAmount,
                                 BigDecimal receivedAmount, BigDecimal outstandingAmount, BigDecimal collectionRatePercent) { }
