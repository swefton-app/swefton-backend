package com.swefton.backend.modules.trainer.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TrainerPackageRequest(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 2000) String description,
        @NotNull @Min(1) @Max(14) Integer sessionsPerWeek,
        @NotNull @Min(1) @Max(365) Integer durationDays,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        boolean active) {
}
