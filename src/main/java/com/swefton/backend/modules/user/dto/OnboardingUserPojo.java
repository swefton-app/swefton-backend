package com.swefton.backend.modules.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OnboardingUserPojo {

    @Valid
    @NotNull
    private UserProfilePojo profile;

    @Valid
    @NotNull
    private UserPreferencesPojo preferences;

    @Valid
    @NotNull
    private UserAddressPojo address;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean onboardingCompleted;
}
