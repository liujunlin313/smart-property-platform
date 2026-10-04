package com.smartproperty.platform.notification.charge;

import java.math.BigDecimal;

/** Raw billing row used only to compose recipient-scoped charge payloads. */
public record ChargeNoticeRow(Long personId, Long propertyUnitId, String propertyNo, String communityName, String buildingNo, Long billId, String billNo,
                              String billStatus, BigDecimal billOutstandingAmount, Long feeTypeId,
                              String feeCode, String feeName, BigDecimal itemAmount) { }
