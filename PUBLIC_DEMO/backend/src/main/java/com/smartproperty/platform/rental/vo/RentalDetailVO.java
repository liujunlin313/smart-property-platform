package com.smartproperty.platform.rental.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record RentalDetailVO(Long rentalListingId, String fullLocation, String layoutType, String layoutName, BigDecimal grossArea,
                             String maskedOwnerName, String listingMode, String agencyName,
                             String authorizationStatus, BigDecimal monthlyRent, BigDecimal depositAmount,
                             LocalDate availableFrom, LocalDateTime publishedAt, LocalDateTime closedAt,
                             String status, Long listingDays, String listingTitle, String description) { }
