package com.swefton.backend.modules.user.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.modules.user.dto.OnboardingUserPojo;
import com.swefton.backend.modules.user.dto.UserAddressPojo;
import com.swefton.backend.modules.user.dto.UserPreferencesPojo;
import com.swefton.backend.modules.user.dto.UserProfilePojo;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.entity.UserAddress;
import com.swefton.backend.modules.user.entity.UserPreferences;
import com.swefton.backend.modules.user.entity.UserProfile;
import com.swefton.backend.modules.user.repository.UserAddressRepository;
import com.swefton.backend.modules.user.repository.UserPreferencesRepository;
import com.swefton.backend.modules.user.repository.UserProfileRepository;
import com.swefton.backend.modules.user.repository.UserRepository;
import com.swefton.backend.session.ISessionUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final UserAddressRepository userAddressRepository;
    private final ISessionUser sessionUser;

    @Override
    @Transactional
    public OnboardingUserPojo completeOnboarding(OnboardingUserPojo request) {
        User user = currentUser();

        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> newProfile(user));
        profile.updateFrom(request.getProfile());
        profile = userProfileRepository.saveAndFlush(profile);

        UserPreferences preferences = userPreferencesRepository.findByUserId(user.getId())
                .orElseGet(() -> newPreferences(user));
        preferences.updateFrom(request.getPreferences());
        preferences = userPreferencesRepository.saveAndFlush(preferences);

        UserAddress address = userAddressRepository.findByUserId(user.getId())
                .stream()
                .findFirst()
                .orElseGet(() -> newAddress(user));
        address.updateFrom(request.getAddress());
        address = userAddressRepository.saveAndFlush(address);

        user.setOnboardingCompleted(true);
        userRepository.saveAndFlush(user);

        return toOnboardingResponse(user, profile, preferences, address);
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingUserPojo getOnboarding() {
        User user = currentUser();
        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(this::onboardingNotFound);
        UserPreferences preferences = userPreferencesRepository.findByUserId(user.getId())
                .orElseThrow(this::onboardingNotFound);
        UserAddress address = userAddressRepository.findByUserId(user.getId())
                .stream()
                .findFirst()
                .orElseThrow(this::onboardingNotFound);

        return toOnboardingResponse(user, profile, preferences, address);
    }

    private User currentUser() {
        return userRepository.findById(sessionUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private UserProfile newProfile(User user) {
        UserProfile profile = new UserProfile();
        profile.setUser(user);
        return profile;
    }

    private UserPreferences newPreferences(User user) {
        UserPreferences preferences = new UserPreferences();
        preferences.setUser(user);
        return preferences;
    }

    private UserAddress newAddress(User user) {
        UserAddress address = new UserAddress();
        address.setUser(user);
        return address;
    }

    private OnboardingUserPojo toOnboardingResponse(
            User user,
            UserProfile profile,
            UserPreferences preferences,
            UserAddress address) {
        OnboardingUserPojo response = new OnboardingUserPojo();
        response.setProfile(new UserProfilePojo(profile));
        response.setPreferences(new UserPreferencesPojo(preferences));
        response.setAddress(new UserAddressPojo(address));
        response.setOnboardingCompleted(user.isOnboardingCompleted());
        return response;
    }

    private ResponseStatusException onboardingNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Onboarding data not found");
    }
}
