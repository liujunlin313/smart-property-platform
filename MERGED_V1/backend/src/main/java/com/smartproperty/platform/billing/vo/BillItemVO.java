package com.smartproperty.platform.billing.vo;

import java.math.BigDecimal;
public record BillItemVO(Integer itemNo, String feeName, String billingMethod, String unitName, BigDecimal quantity, BigDecimal unitPrice,
                         BigDecimal amount, BigDecimal previousReading, BigDecimal currentReading, String note) {}
