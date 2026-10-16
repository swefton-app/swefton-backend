package com.swefton.backend.modules.membership.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;

import com.swefton.backend.modules.payment.dto.CardPaymentRequest;

public record CreateMembershipRequest(
        @NotNull @Positive Long planId,
        @NotNull @Valid CardPaymentRequest payment) {
}
