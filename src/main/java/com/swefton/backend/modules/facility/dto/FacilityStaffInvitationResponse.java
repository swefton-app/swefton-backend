package com.swefton.backend.modules.facility.dto;

import java.time.LocalDateTime;

import com.swefton.backend.modules.facility.entity.FacilityStaffInvitation;

public record FacilityStaffInvitationResponse(
        Long id,
        Long facilityId,
        String email,
        String status,
        LocalDateTime expiresAt,
        LocalDateTime createdAt) {

    public FacilityStaffInvitationResponse(FacilityStaffInvitation invitation) {
        this(
                invitation.getId(),
                invitation.getFacility().getId(),
                invitation.getEmail(),
                invitation.getStatus().name(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt());
    }
}
