package com.smartproperty.platform.dashboard.vo;
import java.math.BigDecimal;
import java.time.LocalDate;
public record OverdueBillVO(Long billId, String propertyNo, String communityName, Integer billingYear,
                            Integer billingMonth, BigDecimal receivableAmount, BigDecimal receivedAmount,
                            BigDecimal outstandingAmount, LocalDate dueDate) {}
