package com.swefton.backend.modules.facility.dto;

import com.swefton.backend.modules.auth.dto.response.AuthResponse;

public record StaffAccountSetupResponse(
        AuthResponse authentication,
        Long facilityId,
        String facilityName) {
}
