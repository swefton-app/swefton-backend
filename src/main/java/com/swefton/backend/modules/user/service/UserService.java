package com.swefton.backend.modules.user.service;

import com.swefton.backend.modules.user.dto.OnboardingUserPojo;
import com.swefton.backend.modules.user.dto.UserAddressPojo;

public interface UserService {

    OnboardingUserPojo completeOnboarding(OnboardingUserPojo request);

    OnboardingUserPojo getOnboarding();

    UserAddressPojo updateAddress(UserAddressPojo request);
}
