package com.swefton.backend.modules.payment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CardPaymentRequest(
        @NotBlank @Size(max = 100) String cardholderName,
        @NotBlank @Pattern(regexp = "[0-9 ]{13,23}") String cardNumber,
        @NotNull @Min(1) @Max(12) Integer expiryMonth,
        @NotNull @Min(2024) @Max(2200) Integer expiryYear,
        @NotBlank @Pattern(regexp = "[0-9]{3,4}") String cvc) {
}
