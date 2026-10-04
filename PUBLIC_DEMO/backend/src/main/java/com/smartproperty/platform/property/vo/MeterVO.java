package com.smartproperty.platform.property.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MeterVO(String feeName, String meterNo, String status,
                      BigDecimal initialReading, LocalDate installedAt) {
}
