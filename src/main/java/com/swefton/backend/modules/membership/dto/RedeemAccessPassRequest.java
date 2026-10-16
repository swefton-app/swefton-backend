package com.swefton.backend.modules.membership.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RedeemAccessPassRequest(
        @NotBlank String token,
        @NotNull @Positive Long userId,
        @NotNull @Positive Long gymId) {
}
