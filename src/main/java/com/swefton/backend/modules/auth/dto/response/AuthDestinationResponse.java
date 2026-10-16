package com.swefton.backend.modules.auth.dto.response;

public record AuthDestinationResponse(
        String role,
        boolean onboardingCompleted,
        Long facilityId,
        String category) {
}
