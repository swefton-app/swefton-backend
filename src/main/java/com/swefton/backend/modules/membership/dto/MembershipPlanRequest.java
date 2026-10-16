package com.swefton.backend.modules.membership.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record MembershipPlanRequest(
        @NotNull @Min(1) @Max(7) Integer visitsPerWeek,
        @NotNull @Min(1) @Max(36) Integer durationMonths,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
        boolean active) {
}
