package com.smartproperty.platform.residency.dto;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
public record MoveOutRequest(@NotNull LocalDate moveOutDate) { }
