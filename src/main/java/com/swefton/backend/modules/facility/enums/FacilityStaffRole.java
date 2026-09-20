package com.swefton.backend.modules.facility.enums;

import java.util.List;
import java.util.Locale;

public enum FacilityStaffRole {

    MANAGER("Manager"),
    RECEPTIONIST("Receptionist"),
    TRAINER("Trainer"),
    INSTRUCTOR("Instructor"),
    ADMINISTRATIVE_STAFF("Administrative Staff"),
    GENERAL_STAFF("General Staff");

    private static final List<FacilityStaffRole> GYM_ROLES = List.of(
            MANAGER,
            RECEPTIONIST,
            TRAINER,
            INSTRUCTOR,
            ADMINISTRATIVE_STAFF,
            GENERAL_STAFF);

    private final String label;

    FacilityStaffRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static List<FacilityStaffRole> forCategory(String category) {
        if (category == null) {
            return List.of();
        }

        return switch (category.trim().toUpperCase(Locale.ROOT)) {
            case FacilityCategory.GYM -> GYM_ROLES;
            default -> List.of();
        };
    }

    public boolean supportsCategory(String category) {
        return forCategory(category).contains(this);
    }
}
