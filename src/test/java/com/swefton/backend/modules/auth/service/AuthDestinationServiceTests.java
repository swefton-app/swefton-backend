package com.swefton.backend.modules.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.swefton.backend.modules.auth.dto.response.AuthDestinationResponse;
import com.swefton.backend.modules.facility.entity.Facility;
import com.swefton.backend.modules.facility.repository.FacilityRepository;
import com.swefton.backend.modules.user.entity.Role;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.enums.RoleCode;
import com.swefton.backend.modules.user.repository.UserRepository;
import com.swefton.backend.session.ISessionUser;

@ExtendWith(MockitoExtension.class)
class AuthDestinationServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private ISessionUser sessionUser;

    private AuthDestinationService service;

    @BeforeEach
    void setUp() {
        service = new AuthDestinationService(userRepository, facilityRepository, sessionUser);
    }

    @Test
    void facilityOwnerDestinationUsesFirstFacilityCategoryFromDatabase() {
        Role role = new Role();
        role.setCode(RoleCode.FACILITY_OWNER);
        User owner = new User();
        owner.setId(38L);
        owner.setRole(role);
        owner.setOnboardingCompleted(true);
        Facility facility = new Facility();
        facility.setId(1L);
        facility.setOwner(owner);
        facility.setCategory("GYM");

        when(sessionUser.getUserId()).thenReturn(38L);
        when(userRepository.findById(38L)).thenReturn(Optional.of(owner));
        when(facilityRepository.findFirstByOwnerIdAndDeletedAtIsNullOrderByCreatedAtAsc(38L))
                .thenReturn(Optional.of(facility));

        AuthDestinationResponse response = service.resolve();

        assertThat(response.role()).isEqualTo(RoleCode.FACILITY_OWNER);
        assertThat(response.onboardingCompleted()).isTrue();
        assertThat(response.facilityId()).isEqualTo(1L);
        assertThat(response.category()).isEqualTo("GYM");
    }
}
