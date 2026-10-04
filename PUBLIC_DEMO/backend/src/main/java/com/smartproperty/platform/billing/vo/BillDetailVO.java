package com.smartproperty.platform.billing.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
public record BillDetailVO(Long billId, String billNo, String propertyNo, String communityName,
                           String buildingName, String unitName, Integer billingYear, Integer billingMonth,
                           LocalDateTime issuedAt,
                           BigDecimal receivableAmount, BigDecimal receivedAmount, BigDecimal outstandingAmount,
                           LocalDate dueDate, String status, List<BillItemVO> items, List<PaymentVO> payments) {}
