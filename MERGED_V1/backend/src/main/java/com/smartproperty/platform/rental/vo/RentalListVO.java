package com.smartproperty.platform.rental.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record RentalListVO(Long rentalListingId, Long communityId, String communityName, String buildingName,
                           String unitName, String propertyNo, String fullLocation, String layoutType, String layoutName,
                           BigDecimal grossArea, String listingMode, Long agencyId, String agencyName,
                           BigDecimal monthlyRent, LocalDate availableFrom, LocalDateTime publishedAt,
                           LocalDateTime closedAt, String status, Long listingDays) { }
