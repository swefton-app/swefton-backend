package com.swefton.backend.modules.machine.dto;

import java.time.LocalDate;

import com.swefton.backend.modules.machine.enums.FacilityMachineStatus;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateFacilityMachineRequest(
        @NotNull @Positive Integer quantity,
        @NotNull FacilityMachineStatus status,
        @Size(max = 1000) String notes,
        @FutureOrPresent LocalDate expectedArrivalDate) {
}
