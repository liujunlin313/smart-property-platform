package com.smartproperty.platform.billing.fee.vo;
import java.math.BigDecimal;
import java.util.List;
public record OverdueOverviewVO(BigDecimal outstandingAmount, long overduePropertyCount, long overdueBillCount, List<OverduePropertyVO> topProperties) { }
