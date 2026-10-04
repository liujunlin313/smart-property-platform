package com.smartproperty.platform.billing.fee.vo;
import java.math.BigDecimal;
import java.time.LocalDate;
public record OverdueBillVO(Long billId, String billNo, String communityName, String fullLocation,
                            Integer billingYear, Integer billingMonth, BigDecimal receivableAmount,
                            BigDecimal receivedAmount, BigDecimal outstandingAmount, LocalDate dueDate) { }
