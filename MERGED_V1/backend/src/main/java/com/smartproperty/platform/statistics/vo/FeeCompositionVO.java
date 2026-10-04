package com.smartproperty.platform.statistics.vo;
import java.math.BigDecimal;
public record FeeCompositionVO(String feeCode, String feeName, BigDecimal receivableAmount) {}
