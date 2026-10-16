package com.swefton.backend.modules.gym.dto;

import com.swefton.backend.modules.facility.dto.CreateFacilityRequest;
import com.swefton.backend.modules.user.dto.OnboardingUserPojo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CompleteGymOnboardingRequest(
        @Valid @NotNull OnboardingUserPojo onboarding,
        @Valid @NotNull CreateFacilityRequest facility) {
}
