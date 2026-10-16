package com.swefton.backend.modules.pricing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record PriceOfferRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal salePrice,
        @NotNull LocalDateTime startsAt,
        @NotNull LocalDateTime endsAt) {
}
