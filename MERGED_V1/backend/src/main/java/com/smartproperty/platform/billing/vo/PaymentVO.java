package com.smartproperty.platform.billing.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
public record PaymentVO(String paymentNo, String payerName, BigDecimal amount, LocalDateTime paidAt,
                        String paymentMethod, String status) {}
