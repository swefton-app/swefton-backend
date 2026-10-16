package com.swefton.backend.modules.facility.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.swefton.backend.modules.document.entity.UserDocument;
import com.swefton.backend.modules.document.enums.DocumentType;
import com.swefton.backend.modules.document.repository.UserDocumentRepository;
import com.swefton.backend.modules.facility.dto.CompleteStaffOnboardingRequest;
import com.swefton.backend.modules.facility.dto.FacilityStaffResponse;
import com.swefton.backend.modules.facility.entity.Facility;
import com.swefton.backend.modules.facility.entity.FacilityStaff;
import com.swefton.backend.modules.facility.entity.FacilityStaffInvitation;
import com.swefton.backend.modules.facility.enums.FacilityStaffInvitationStatus;
import com.swefton.backend.modules.facility.enums.FacilityStaffRole;
import com.swefton.backend.modules.facility.repository.FacilityStaffInvitationRepository;
import com.swefton.backend.modules.facility.repository.FacilityStaffRepository;
import com.swefton.backend.modules.user.dto.OnboardingUserPojo;
import com.swefton.backend.modules.user.dto.UserAddressPojo;
import com.swefton.backend.modules.user.dto.UserPreferencesPojo;
import com.swefton.backend.modules.user.dto.UserProfilePojo;
import com.swefton.backend.modules.user.entity.Role;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.enums.RoleCode;
import com.swefton.backend.modules.user.repository.UserRepository;
import com.swefton.backend.modules.user.service.UserService;
import com.swefton.backend.session.ISessionUser;

@ExtendWith(MockitoExtension.class)
class StaffOnboardingServiceTests {

    @Mock private FacilityStaffInvitationRepository invitationRepository;
    @Mock private FacilityStaffRepository staffRepository;
    @Mock private UserDocumentRepository userDocumentRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserService userService;
    @Mock private ISessionUser sessionUser;

    private StaffOnboardingService service;

    @BeforeEach
    void setUp() {
        service = new StaffOnboardingService(
                invitationRepository,
                staffRepository,
                userDocumentRepository,
                userRepository,
                userService,
                sessionUser);
    }

    @Test
    void instructorOnboardingRequiresDocumentsCreatesMembershipAndRemovesPrice() {
        User user = staffUser(20L);
        Facility facility = new Facility();
        facility.setId(10L);
        facility.setCategory("GYM");
        FacilityStaffInvitation invitation = new FacilityStaffInvitation();
        invitation.setFacility(facility);
        invitation.setUser(user);
        invitation.setStatus(FacilityStaffInvitationStatus.ACCOUNT_CREATED);
        OnboardingUserPojo onboarding = onboarding();

        when(sessionUser.getUserId()).thenReturn(20L);
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));
        when(invitationRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                20L, FacilityStaffInvitationStatus.ACCOUNT_CREATED)).thenReturn(Optional.of(invitation));
        when(userDocumentRepository.findByUserIdAndTypeAndDeletedAtIsNull(20L, DocumentType.CV))
                .thenReturn(Optional.of(new UserDocument()));
        when(userDocumentRepository.findByUserIdAndTypeAndDeletedAtIsNull(20L, DocumentType.LICENCE))
                .thenReturn(Optional.of(new UserDocument()));
        when(staffRepository.findByFacilityIdAndUserId(10L, 20L)).thenReturn(Optional.empty());
        when(staffRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(FacilityStaff.class)))
                .thenAnswer(invocation -> {
                    FacilityStaff staff = invocation.getArgument(0);
                    staff.setId(30L);
                    staff.prePersist();
                    return staff;
                });

        FacilityStaffResponse response = service.complete(
                new CompleteStaffOnboardingRequest(FacilityStaffRole.INSTRUCTOR, onboarding));

        assertThat(response.role()).isEqualTo("INSTRUCTOR");
        assertThat(onboarding.getProfile().getPrice()).isNull();
        assertThat(invitation.getStatus()).isEqualTo(FacilityStaffInvitationStatus.COMPLETED);
        verify(userService).completeOnboarding(onboarding);
    }

    private User staffUser(Long id) {
        Role role = new Role();
        role.setCode(RoleCode.STAFF);
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }

    private OnboardingUserPojo onboarding() {
        UserProfilePojo profile = new UserProfilePojo();
        profile.setFirstName("Alex");
        profile.setLastName("Coach");
        profile.setPrice(new BigDecimal("50.00"));
        UserAddressPojo address = new UserAddressPojo();
        UserPreferencesPojo preferences = new UserPreferencesPojo();
        OnboardingUserPojo onboarding = new OnboardingUserPojo();
        onboarding.setProfile(profile);
        onboarding.setAddress(address);
        onboarding.setPreferences(preferences);
        return onboarding;
    }
}
