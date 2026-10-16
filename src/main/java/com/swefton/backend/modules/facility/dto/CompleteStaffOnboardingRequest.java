package com.swefton.backend.modules.facility.dto;

import com.swefton.backend.modules.facility.enums.FacilityStaffRole;
import com.swefton.backend.modules.user.dto.OnboardingUserPojo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CompleteStaffOnboardingRequest(
        @NotNull FacilityStaffRole role,
        @Valid @NotNull OnboardingUserPojo onboarding) {
}
