package com.swefton.backend.modules.gym.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateGymLocationRequest(
        @NotBlank @Size(max = 255) String addressLine,
        @NotBlank @Size(max = 100) String city,
        @Size(max = 100) String state,
        @Size(max = 20) String postalCode,
        @NotBlank @Size(max = 100) String country,
        @NotBlank @Size(max = 500) String formattedAddress,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") @Digits(integer = 3, fraction = 7)
        BigDecimal latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") @Digits(integer = 3, fraction = 7)
        BigDecimal longitude) {
}
