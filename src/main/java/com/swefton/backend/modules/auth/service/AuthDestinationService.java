package com.swefton.backend.modules.auth.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.modules.auth.dto.response.AuthDestinationResponse;
import com.swefton.backend.modules.facility.entity.Facility;
import com.swefton.backend.modules.facility.enums.FacilityCategory;
import com.swefton.backend.modules.facility.repository.FacilityRepository;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.enums.RoleCode;
import com.swefton.backend.modules.user.repository.UserRepository;
import com.swefton.backend.session.ISessionUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthDestinationService {

    private final UserRepository userRepository;
    private final FacilityRepository facilityRepository;
    private final ISessionUser sessionUser;

    @Transactional(readOnly = true)
    public AuthDestinationResponse resolve() {
        User user = userRepository.findById(sessionUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String role = user.getRole().getCode();

        if (RoleCode.FACILITY_OWNER.equals(role)) {
            Facility firstFacility = facilityRepository
                    .findFirstByOwnerIdAndDeletedAtIsNullOrderByCreatedAtAsc(user.getId())
                    .orElse(null);
            if (firstFacility != null) {
                if (!FacilityCategory.exists(firstFacility.getCategory())) {
                    throw new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Facility category is invalid");
                }
                return new AuthDestinationResponse(
                        role,
                        user.isOnboardingCompleted(),
                        firstFacility.getId(),
                        firstFacility.getCategory());
            }
        }

        return new AuthDestinationResponse(
                role,
                user.isOnboardingCompleted(),
                null,
                null);
    }
}
