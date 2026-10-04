package com.smartproperty.platform.asset.dto.response;

import java.math.BigDecimal;

public record LayoutTemplateResponse(Long layoutTemplateId, String layoutCode, String layoutName,
        String layoutType, BigDecimal standardArea, String description, String status) {
}
