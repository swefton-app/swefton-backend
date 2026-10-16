package com.swefton.backend.modules.facility.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.infrastructure.email.EmailService;
import com.swefton.backend.modules.auth.dto.response.AuthResponse;
import com.swefton.backend.modules.auth.service.AuthTokenService;
import com.swefton.backend.modules.facility.dto.CreateStaffInvitationRequest;
import com.swefton.backend.modules.facility.dto.SetStaffPasswordRequest;
import com.swefton.backend.modules.facility.dto.StaffAccountSetupResponse;
import com.swefton.backend.modules.facility.entity.Facility;
import com.swefton.backend.modules.facility.entity.FacilityStaffInvitation;
import com.swefton.backend.modules.facility.enums.FacilityStaffInvitationStatus;
import com.swefton.backend.modules.facility.repository.FacilityRepository;
import com.swefton.backend.modules.facility.repository.FacilityStaffInvitationRepository;
import com.swefton.backend.modules.user.entity.Role;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.enums.RoleCode;
import com.swefton.backend.modules.user.repository.RoleRepository;
import com.swefton.backend.modules.user.repository.UserRepository;
import com.swefton.backend.session.ISessionUser;

@ExtendWith(MockitoExtension.class)
class FacilityStaffInvitationServiceTests {

    @Mock private FacilityStaffInvitationRepository invitationRepository;
    @Mock private FacilityRepository facilityRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthTokenService authTokenService;
    @Mock private EmailService emailService;
    @Mock private ISessionUser sessionUser;

    private FacilityStaffInvitationService service;

    @BeforeEach
    void setUp() {
        service = new FacilityStaffInvitationService(
                invitationRepository,
                facilityRepository,
                userRepository,
                roleRepository,
                passwordEncoder,
                authTokenService,
                emailService,
                sessionUser,
                "https://app.swefton.test/",
                72);
    }

    @Test
    void ownerInvitationStoresAHashAndEmailsAOneTimeOnboardingLink() {
        User owner = user(1L, RoleCode.FACILITY_OWNER);
        Facility facility = facility(10L, owner);
        when(facilityRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(facility));
        when(sessionUser.getUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(userRepository.existsByEmailIgnoreCase("coach@example.com")).thenReturn(false);
        when(invitationRepository.findByFacilityIdAndEmailIgnoreCase(10L, "coach@example.com"))
                .thenReturn(Optional.empty());
        when(invitationRepository.saveAndFlush(any(FacilityStaffInvitation.class))).thenAnswer(invocation -> {
            FacilityStaffInvitation saved = invocation.getArgument(0);
            saved.setId(50L);
            saved.prePersist();
            return saved;
        });

        var response = service.invite(10L, new CreateStaffInvitationRequest(" Coach@Example.com "));

        assertThat(response.email()).isEqualTo("coach@example.com");
        ArgumentCaptor<FacilityStaffInvitation> invitation = ArgumentCaptor.forClass(FacilityStaffInvitation.class);
        verify(invitationRepository).saveAndFlush(invitation.capture());
        assertThat(invitation.getValue().getTokenHash()).hasSize(64);
        assertThat(invitation.getValue().getStatus()).isEqualTo(FacilityStaffInvitationStatus.PENDING);

        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendStaffInvitation(
                org.mockito.ArgumentMatchers.eq("coach@example.com"),
                org.mockito.ArgumentMatchers.eq("Power House"),
                link.capture(),
                org.mockito.ArgumentMatchers.eq(72L));
        assertThat(link.getValue()).startsWith("https://app.swefton.test/staff/setup?token=");
        assertThat(link.getValue()).doesNotContain(invitation.getValue().getTokenHash());
    }

    @Test
    void passwordSetupCreatesAConfirmedStaffAccountAndConsumesTheToken() {
        User owner = user(1L, RoleCode.FACILITY_OWNER);
        Facility facility = facility(10L, owner);
        FacilityStaffInvitation invitation = new FacilityStaffInvitation();
        invitation.setId(50L);
        invitation.setFacility(facility);
        invitation.setInvitedBy(owner);
        invitation.setEmail("coach@example.com");
        invitation.setTokenHash("stored-hash");
        invitation.setStatus(FacilityStaffInvitationStatus.PENDING);
        invitation.setExpiresAt(java.time.LocalDateTime.now().plusHours(1));
        Role staffRole = new Role();
        staffRole.setCode(RoleCode.STAFF);

        when(invitationRepository.findByTokenHash(anyString())).thenReturn(Optional.of(invitation));
        when(userRepository.existsByEmailIgnoreCase("coach@example.com")).thenReturn(false);
        when(roleRepository.findByCode(RoleCode.STAFF)).thenReturn(Optional.of(staffRole));
        when(passwordEncoder.encode("strong-password")).thenReturn("encoded");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(20L);
            return saved;
        });
        AuthResponse auth = AuthResponse.builder().accessToken("access").refreshToken("refresh").role(RoleCode.STAFF).build();
        when(authTokenService.issue(any(User.class), org.mockito.ArgumentMatchers.eq(true))).thenReturn(auth);

        StaffAccountSetupResponse response = service.setPassword(
                "raw-token",
                new SetStaffPasswordRequest("strong-password", "strong-password"));

        assertThat(response.authentication()).isSameAs(auth);
        assertThat(invitation.getStatus()).isEqualTo(FacilityStaffInvitationStatus.ACCOUNT_CREATED);
        assertThat(invitation.getTokenHash()).isNull();
        assertThat(invitation.getUser()).isNotNull();
        assertThat(invitation.getUser().isEmailConfirmed()).isTrue();
        assertThat(invitation.getUser().getRole()).isSameAs(staffRole);
        assertThat(invitation.getUser().getPasswordHash()).isEqualTo("encoded");
    }

    @Test
    void passwordSetupRejectsDifferentPasswordsBeforeUsingTheInvitation() {
        assertThatThrownBy(() -> service.setPassword(
                "raw-token",
                new SetStaffPasswordRequest("strong-password", "different-password")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Passwords do not match");
    }

    private User user(Long id, String roleCode) {
        Role role = new Role();
        role.setCode(roleCode);
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }

    private Facility facility(Long id, User owner) {
        Facility facility = new Facility();
        facility.setId(id);
        facility.setOwner(owner);
        facility.setName("Power House");
        facility.setCategory("GYM");
        return facility;
    }
}
