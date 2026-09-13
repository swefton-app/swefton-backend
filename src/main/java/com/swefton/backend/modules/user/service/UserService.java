package com.swefton.backend.modules.user.service;

import com.swefton.backend.modules.user.dto.OnboardingUserPojo;

public interface UserService {

    OnboardingUserPojo completeOnboarding(OnboardingUserPojo request);

    OnboardingUserPojo getOnboarding();
}
