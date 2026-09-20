package com.swefton.backend.modules.facility.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetStaffPasswordRequest(
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank @Size(min = 8, max = 100) String confirmPassword) {
}
