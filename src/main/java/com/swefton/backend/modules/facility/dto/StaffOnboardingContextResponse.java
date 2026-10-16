package com.swefton.backend.modules.facility.dto;

import java.util.List;

public record StaffOnboardingContextResponse(
        Long facilityId,
        String facilityName,
        String facilityCategory,
        String email,
        List<FacilityStaffRoleResponse> roles) {
}
