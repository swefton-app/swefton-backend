package com.swefton.backend.modules.gym.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swefton.backend.modules.gym.dto.CompleteGymOnboardingRequest;
import com.swefton.backend.modules.gym.dto.CompleteGymOnboardingResponse;
import com.swefton.backend.modules.gym.dto.GymResponse;
import com.swefton.backend.modules.user.dto.OnboardingUserPojo;
import com.swefton.backend.modules.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GymOnboardingService {

    private final UserService userService;
    private final GymService gymService;

    @Transactional
    public CompleteGymOnboardingResponse complete(CompleteGymOnboardingRequest request) {
        OnboardingUserPojo onboarding = userService.completeOnboarding(request.onboarding());
        GymResponse gym = gymService.create(request.facility());
        return new CompleteGymOnboardingResponse(onboarding, gym);
    }
}
