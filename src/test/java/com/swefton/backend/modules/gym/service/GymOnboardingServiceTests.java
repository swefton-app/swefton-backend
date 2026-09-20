package com.swefton.backend.modules.gym.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.swefton.backend.modules.gym.dto.CompleteGymOnboardingRequest;
import com.swefton.backend.modules.gym.dto.CompleteGymOnboardingResponse;
import com.swefton.backend.modules.facility.dto.CreateFacilityRequest;
import com.swefton.backend.modules.gym.dto.GymResponse;
import com.swefton.backend.modules.gym.enums.GymStatus;
import com.swefton.backend.modules.gym.enums.GymType;
import com.swefton.backend.modules.user.dto.OnboardingUserPojo;
import com.swefton.backend.modules.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class GymOnboardingServiceTests {

    @Mock
    private UserService userService;

    @Mock
    private GymService gymService;

    private GymOnboardingService gymOnboardingService;

    @BeforeEach
    void setUp() {
        gymOnboardingService = new GymOnboardingService(userService, gymService);
    }

    @Test
    void completeSavesOwnerOnboardingAndGymTogether() {
        OnboardingUserPojo requestOnboarding = new OnboardingUserPojo();
        OnboardingUserPojo savedOnboarding = new OnboardingUserPojo();
        savedOnboarding.setOnboardingCompleted(true);
        CreateFacilityRequest facilityRequest = new CreateFacilityRequest(
                "Power House", "GYM", null, "hello@powerhouse.test", null, null,
                "1 Main Street", "Tirana", null, null, "Albania",
                "1 Main Street, Tirana, Albania",
                new BigDecimal("41.3275000"), new BigDecimal("19.8187000"),
                GymType.COMMERCIAL, 250, false, null, null, List.of());
        GymResponse savedGym = new GymResponse(
                7L, 8L, 42L, "GYM", "Power House", null, "hello@powerhouse.test", null, null,
                "1 Main Street", "Tirana", null, null, "Albania",
                "1 Main Street, Tirana, Albania",
                new BigDecimal("41.3275000"), new BigDecimal("19.8187000"),
                GymType.COMMERCIAL, 250, false, GymStatus.DRAFT,
                null, null, List.of(), BigDecimal.ZERO, 0, LocalDateTime.now());
        CompleteGymOnboardingRequest request =
                new CompleteGymOnboardingRequest(requestOnboarding, facilityRequest);

        when(userService.completeOnboarding(requestOnboarding)).thenReturn(savedOnboarding);
        when(gymService.create(facilityRequest)).thenReturn(savedGym);

        CompleteGymOnboardingResponse response = gymOnboardingService.complete(request);

        assertThat(response.onboarding().isOnboardingCompleted()).isTrue();
        assertThat(response.gym()).isSameAs(savedGym);
        verify(userService).completeOnboarding(requestOnboarding);
        verify(gymService).create(facilityRequest);
    }
}
