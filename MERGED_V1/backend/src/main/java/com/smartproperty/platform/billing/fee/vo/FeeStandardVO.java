package com.smartproperty.platform.billing.fee.vo;
import java.math.BigDecimal;
import java.time.LocalDate;
public record FeeStandardVO(Long feeStandardId, String communityName, String feeName, String billingMethod,
                            BigDecimal unitPrice, String unitName, LocalDate effectiveFrom, LocalDate effectiveTo, String status) { }
