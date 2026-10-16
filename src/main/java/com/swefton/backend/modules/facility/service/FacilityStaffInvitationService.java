package com.swefton.backend.modules.facility.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.infrastructure.email.EmailService;
import com.swefton.backend.modules.auth.dto.response.AuthResponse;
import com.swefton.backend.modules.auth.service.AuthTokenService;
import com.swefton.backend.modules.facility.dto.CreateStaffInvitationRequest;
import com.swefton.backend.modules.facility.dto.FacilityStaffInvitationResponse;
import com.swefton.backend.modules.facility.dto.SetStaffPasswordRequest;
import com.swefton.backend.modules.facility.dto.StaffAccountSetupResponse;
import com.swefton.backend.modules.facility.dto.StaffInvitationDetailsResponse;
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

@Service
public class FacilityStaffInvitationService {

    private static final int TOKEN_BYTES = 32;
    private final FacilityStaffInvitationRepository invitationRepository;
    private final FacilityRepository facilityRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;
    private final EmailService emailService;
    private final ISessionUser sessionUser;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String frontendBaseUrl;
    private final long invitationTtlHours;

    public FacilityStaffInvitationService(
            FacilityStaffInvitationRepository invitationRepository,
            FacilityRepository facilityRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthTokenService authTokenService,
            EmailService emailService,
            ISessionUser sessionUser,
            @Value("${app.frontend.base-url}") String frontendBaseUrl,
            @Value("${app.staff-invitation.ttl-hours:72}") long invitationTtlHours) {
        this.invitationRepository = invitationRepository;
        this.facilityRepository = facilityRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authTokenService = authTokenService;
        this.emailService = emailService;
        this.sessionUser = sessionUser;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
        if (invitationTtlHours <= 0) {
            throw new IllegalArgumentException("Staff invitation lifetime must be positive");
        }
        this.invitationTtlHours = invitationTtlHours;
    }

    @Transactional
    public FacilityStaffInvitationResponse invite(Long facilityId, CreateStaffInvitationRequest request) {
        Facility facility = managedFacility(facilityId);
        User inviter = currentUser();
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This email already has a Swefton account");
        }
        if (invitationRepository.existsByEmailIgnoreCaseAndStatus(
                email, FacilityStaffInvitationStatus.ACCOUNT_CREATED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This staff account is already being onboarded");
        }

        String token = newToken();
        FacilityStaffInvitation invitation = invitationRepository
                .findByFacilityIdAndEmailIgnoreCase(facilityId, email)
                .orElseGet(FacilityStaffInvitation::new);
        invitation.setFacility(facility);
        invitation.setInvitedBy(inviter);
        invitation.setUser(null);
        invitation.setEmail(email);
        invitation.setTokenHash(hash(token));
        invitation.setStatus(FacilityStaffInvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now().plusHours(invitationTtlHours));
        invitation.setAccountCreatedAt(null);
        invitation.setCompletedAt(null);
        invitation = invitationRepository.saveAndFlush(invitation);

        String onboardingUrl = frontendBaseUrl + "/staff/setup?token=" + token;
        try {
            emailService.sendStaffInvitation(
                    email,
                    facility.getName(),
                    onboardingUrl,
                    invitationTtlHours);
        } catch (MailException | IllegalStateException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Invitation email could not be sent. Please try again later.",
                    exception);
        }
        return new FacilityStaffInvitationResponse(invitation);
    }

    @Transactional(readOnly = true)
    public List<FacilityStaffInvitationResponse> list(Long facilityId) {
        managedFacility(facilityId);
        return invitationRepository.findAllByFacilityIdOrderByCreatedAtDesc(facilityId)
                .stream()
                .map(FacilityStaffInvitationResponse::new)
                .toList();
    }

    @Transactional
    public void revoke(Long facilityId, Long invitationId) {
        managedFacility(facilityId);
        FacilityStaffInvitation invitation = invitationRepository.findById(invitationId)
                .filter(item -> item.getFacility().getId().equals(facilityId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));
        if (invitation.getStatus() == FacilityStaffInvitationStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Completed invitations cannot be cancelled");
        }
        invitation.setStatus(FacilityStaffInvitationStatus.REVOKED);
        invitation.setTokenHash(null);
        invitationRepository.save(invitation);
    }

    @Transactional(readOnly = true)
    public StaffInvitationDetailsResponse details(String token) {
        FacilityStaffInvitation invitation = validPendingInvitation(token);
        return new StaffInvitationDetailsResponse(
                invitation.getEmail(),
                invitation.getFacility().getId(),
                invitation.getFacility().getName(),
                invitation.getExpiresAt());
    }

    @Transactional
    public StaffAccountSetupResponse setPassword(String token, SetStaffPasswordRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }
        FacilityStaffInvitation invitation = validPendingInvitation(token);
        if (userRepository.existsByEmailIgnoreCase(invitation.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This email already has a Swefton account");
        }

        Role staffRole = roleRepository.findByCode(RoleCode.STAFF)
                .orElseThrow(() -> new IllegalStateException("STAFF role is not configured"));
        User user = new User();
        user.setEmail(invitation.getEmail());
        user.setAuthSubject(invitation.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(staffRole);
        user.setEnabled(true);
        user.setEmailConfirmed(true);
        user.setOnboardingCompleted(false);
        user = userRepository.saveAndFlush(user);

        invitation.setUser(user);
        invitation.setTokenHash(null);
        invitation.setStatus(FacilityStaffInvitationStatus.ACCOUNT_CREATED);
        invitation.setAccountCreatedAt(LocalDateTime.now());
        invitationRepository.saveAndFlush(invitation);

        AuthResponse authentication = authTokenService.issue(user, true);
        return new StaffAccountSetupResponse(
                authentication,
                invitation.getFacility().getId(),
                invitation.getFacility().getName());
    }

    private Facility managedFacility(Long facilityId) {
        Facility facility = facilityRepository.findByIdAndDeletedAtIsNull(facilityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facility not found"));
        User user = currentUser();
        String role = user.getRole() == null ? null : user.getRole().getCode();
        boolean owner = facility.getOwner().getId().equals(user.getId());
        boolean platformAdmin = RoleCode.ADMIN.equals(role);
        if (!owner && !platformAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot manage this facility's staff");
        }
        return facility;
    }

    private User currentUser() {
        return userRepository.findById(sessionUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private FacilityStaffInvitation validPendingInvitation(String token) {
        if (token == null || token.isBlank()) {
            throw invalidInvitation();
        }
        FacilityStaffInvitation invitation = invitationRepository.findByTokenHash(hash(token))
                .orElseThrow(this::invalidInvitation);
        if (invitation.getStatus() != FacilityStaffInvitationStatus.PENDING
                || !invitation.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw invalidInvitation();
        }
        return invitation;
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException invalidInvitation() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation link is invalid or expired");
    }
}
