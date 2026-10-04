package com.smartproperty.platform.billing.fee.vo;
import java.math.BigDecimal;
public record OverduePropertyVO(Long propertyUnitId, String fullLocation, BigDecimal outstandingAmount, long overdueBillCount) { }
