package com.smartproperty.platform.billing.vo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
public record BillDetailBaseVO(Long billId, String billNo, String propertyNo, String communityName, String buildingName, String unitName, Integer billingYear, Integer billingMonth, LocalDateTime issuedAt, BigDecimal receivableAmount, BigDecimal receivedAmount, BigDecimal outstandingAmount, LocalDate dueDate, String status) {}
