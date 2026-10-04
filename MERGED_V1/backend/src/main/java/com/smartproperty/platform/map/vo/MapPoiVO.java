package com.smartproperty.platform.map.vo;

import java.math.BigDecimal;

public record MapPoiVO(String poiId, String name, String type, String address,
                       BigDecimal longitude, BigDecimal latitude) { }
