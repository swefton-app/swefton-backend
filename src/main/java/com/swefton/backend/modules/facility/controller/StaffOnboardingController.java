package com.swefton.backend.modules.facility.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swefton.backend.modules.facility.dto.CompleteStaffOnboardingRequest;
import com.swefton.backend.modules.facility.dto.FacilityStaffResponse;
import com.swefton.backend.modules.facility.dto.StaffOnboardingContextResponse;
import com.swefton.backend.modules.facility.service.StaffOnboardingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('STAFF')")
@RequestMapping("/api/v1/staff/onboarding")
public class StaffOnboardingController {

    private final StaffOnboardingService onboardingService;

    @GetMapping("/context")
    public ResponseEntity<StaffOnboardingContextResponse> context() {
        return ResponseEntity.ok(onboardingService.context());
    }

    @PostMapping
    public ResponseEntity<FacilityStaffResponse> complete(
            @Valid @RequestBody CompleteStaffOnboardingRequest request) {
        return ResponseEntity.ok(onboardingService.complete(request));
    }
}
