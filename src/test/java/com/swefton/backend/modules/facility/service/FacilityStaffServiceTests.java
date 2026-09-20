package com.swefton.backend.modules.facility.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class FacilityStaffServiceTests {

    private final FacilityStaffService service = new FacilityStaffService();

    @Test
    void gymCategoryReturnsGymStaffRoles() {
        var roles = service.findAvailableRoles("gym");

        assertThat(roles)
                .extracting(role -> role.code())
                .containsExactly(
                        "MANAGER",
                        "RECEPTIONIST",
                        "TRAINER",
                        "INSTRUCTOR",
                        "ADMINISTRATIVE_STAFF",
                        "GENERAL_STAFF");
    }

    @Test
    void configuredCategoryWithoutStaffRolesReturnsEmptyList() {
        assertThat(service.findAvailableRoles("YOGA")).isEmpty();
    }

    @Test
    void invalidCategoryIsRejected() {
        assertThatThrownBy(() -> service.findAvailableRoles("restaurant"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).isEqualTo("Facility category is invalid");
                });
    }
}
