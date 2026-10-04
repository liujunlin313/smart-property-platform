package com.smartproperty.platform.billing.fee.vo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record PaymentListVO(Long paymentId, String paymentNo, String billNo, String communityName,
                            String fullLocation, String payerName, BigDecimal amount, LocalDateTime paidAt,
                            String paymentMethod, String status) { }
