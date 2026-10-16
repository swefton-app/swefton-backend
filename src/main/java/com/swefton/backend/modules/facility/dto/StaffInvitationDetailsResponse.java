package com.swefton.backend.modules.facility.dto;

import java.time.LocalDateTime;

public record StaffInvitationDetailsResponse(
        String email,
        Long facilityId,
        String facilityName,
        LocalDateTime expiresAt) {
}
