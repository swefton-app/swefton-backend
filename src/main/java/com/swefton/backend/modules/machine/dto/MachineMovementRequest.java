package com.swefton.backend.modules.machine.dto;

import com.swefton.backend.modules.machine.enums.MachineMuscleGroup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record MachineMovementRequest(
        @Positive Long id,
        @NotBlank @Size(max = 150) String name,
        @NotNull MachineMuscleGroup muscleGroup,
        @Positive Long videoId,
        @Size(max = 5000) String instructions,
        @PositiveOrZero Integer position) {
}
