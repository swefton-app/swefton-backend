package com.swefton.backend.modules.gym.dto;

import com.swefton.backend.modules.user.dto.OnboardingUserPojo;

public record CompleteGymOnboardingResponse(
        OnboardingUserPojo onboarding,
        GymResponse gym) {
}
