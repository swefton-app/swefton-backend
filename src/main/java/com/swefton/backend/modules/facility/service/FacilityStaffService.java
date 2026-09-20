package com.swefton.backend.modules.facility.service;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.modules.facility.dto.FacilityStaffRoleResponse;
import com.swefton.backend.modules.facility.enums.FacilityCategory;
import com.swefton.backend.modules.facility.enums.FacilityStaffRole;

@Service
public class FacilityStaffService {

    public List<FacilityStaffRoleResponse> findAvailableRoles(String category) {
        if (category == null || category.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Facility category is required");
        }

        String normalizedCategory = category.trim().toUpperCase(Locale.ROOT);
        if (!FacilityCategory.exists(normalizedCategory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Facility category is invalid");
        }

        return FacilityStaffRole.forCategory(normalizedCategory)
                .stream()
                .map(FacilityStaffRoleResponse::new)
                .toList();
    }
}
