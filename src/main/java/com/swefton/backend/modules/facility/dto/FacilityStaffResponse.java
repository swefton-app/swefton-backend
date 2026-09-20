package com.swefton.backend.modules.facility.dto;

import java.time.LocalDateTime;

import com.swefton.backend.modules.facility.entity.FacilityStaff;

public record FacilityStaffResponse(
        Long id,
        Long facilityId,
        Long userId,
        String email,
        String role,
        boolean active,
        LocalDateTime createdAt) {

    public FacilityStaffResponse(FacilityStaff staff) {
        this(
                staff.getId(),
                staff.getFacility().getId(),
                staff.getUser().getId(),
                staff.getUser().getEmail(),
                staff.getRole().name(),
                staff.isActive(),
                staff.getCreatedAt());
    }
}
