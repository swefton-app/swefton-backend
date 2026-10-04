package com.swefton.backend.modules.machine.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record MachineRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 100) String code,
        @Size(max = 5000) String description,
        @NotBlank @Size(max = 40) String facilityCategory,
        @NotEmpty @Size(max = 20) List<@Valid MachineMovementRequest> movements,
        @NotEmpty @Size(max = 20) List<Long> imageIds) {
}
