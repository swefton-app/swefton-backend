package com.swefton.backend.modules.trainer.dto;

import com.swefton.backend.modules.payment.dto.CardPaymentRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TrainerCheckoutRequest(
        @NotNull @Positive Long packageId,
        @NotNull @Valid CardPaymentRequest payment) {
}
