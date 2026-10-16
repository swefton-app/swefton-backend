package com.swefton.backend.modules.facility.dto;

import com.swefton.backend.modules.facility.enums.FacilityStaffRole;

public record FacilityStaffRoleResponse(
        String code,
        String label) {

    public FacilityStaffRoleResponse(FacilityStaffRole role) {
        this(role.name(), role.getLabel());
    }
}
