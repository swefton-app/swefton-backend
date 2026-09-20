package com.swefton.backend.modules.facility.service;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.modules.document.enums.DocumentType;
import com.swefton.backend.modules.document.repository.UserDocumentRepository;
import com.swefton.backend.modules.facility.dto.CompleteStaffOnboardingRequest;
import com.swefton.backend.modules.facility.dto.FacilityStaffResponse;
import com.swefton.backend.modules.facility.dto.FacilityStaffRoleResponse;
import com.swefton.backend.modules.facility.dto.StaffOnboardingContextResponse;
import com.swefton.backend.modules.facility.entity.FacilityStaff;
import com.swefton.backend.modules.facility.entity.FacilityStaffInvitation;
import com.swefton.backend.modules.facility.enums.FacilityStaffInvitationStatus;
import com.swefton.backend.modules.facility.enums.FacilityStaffRole;
import com.swefton.backend.modules.facility.repository.FacilityStaffInvitationRepository;
import com.swefton.backend.modules.facility.repository.FacilityStaffRepository;
import com.swefton.backend.modules.user.dto.OnboardingUserPojo;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.enums.RoleCode;
import com.swefton.backend.modules.user.repository.UserRepository;
import com.swefton.backend.modules.user.service.UserService;
import com.swefton.backend.session.ISessionUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StaffOnboardingService {

    private final FacilityStaffInvitationRepository invitationRepository;
    private final FacilityStaffRepository staffRepository;
    private final UserDocumentRepository userDocumentRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ISessionUser sessionUser;

    @Transactional(readOnly = true)
    public StaffOnboardingContextResponse context() {
        User user = currentStaffUser();
        FacilityStaffInvitation invitation = accountCreatedInvitation(user.getId());
        return new StaffOnboardingContextResponse(
                invitation.getFacility().getId(),
                invitation.getFacility().getName(),
                invitation.getFacility().getCategory(),
                user.getEmail(),
                FacilityStaffRole.forCategory(invitation.getFacility().getCategory())
                        .stream()
                        .map(FacilityStaffRoleResponse::new)
                        .toList());
    }

    @Transactional
    public FacilityStaffResponse complete(CompleteStaffOnboardingRequest request) {
        User user = currentStaffUser();
        FacilityStaffInvitation invitation = accountCreatedInvitation(user.getId());
        FacilityStaffRole staffRole = request.role();
        if (!staffRole.supportsCategory(invitation.getFacility().getCategory())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Staff role is not valid for this facility");
        }

        requireDocument(user.getId(), DocumentType.CV, "Add or create your CV before finishing onboarding");
        if (staffRole == FacilityStaffRole.INSTRUCTOR || staffRole == FacilityStaffRole.TRAINER) {
            requireDocument(user.getId(), DocumentType.LICENCE, "Add your professional licence before finishing onboarding");
        }

        OnboardingUserPojo onboarding = request.onboarding();
        onboarding.getProfile().setPrice(null);
        userService.completeOnboarding(onboarding);

        FacilityStaff staff = staffRepository
                .findByFacilityIdAndUserId(invitation.getFacility().getId(), user.getId())
                .orElseGet(FacilityStaff::new);
        staff.setFacility(invitation.getFacility());
        staff.setUser(user);
        staff.setRole(staffRole);
        staff.setActive(true);
        staff = staffRepository.saveAndFlush(staff);

        invitation.setStatus(FacilityStaffInvitationStatus.COMPLETED);
        invitation.setCompletedAt(LocalDateTime.now());
        invitationRepository.save(invitation);
        return new FacilityStaffResponse(staff);
    }

    private void requireDocument(Long userId, String type, String message) {
        if (userDocumentRepository.findByUserIdAndTypeAndDeletedAtIsNull(userId, type).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private User currentStaffUser() {
        User user = userRepository.findById(sessionUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() == null || !RoleCode.STAFF.equals(user.getRole().getCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only invited staff can use staff onboarding");
        }
        return user;
    }

    private FacilityStaffInvitation accountCreatedInvitation(Long userId) {
        return invitationRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                        userId, FacilityStaffInvitationStatus.ACCOUNT_CREATED)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Staff onboarding invitation not found"));
    }
}
