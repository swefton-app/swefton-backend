package com.swefton.backend.modules.gym.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swefton.backend.modules.facility.dto.CreateFacilityRequest;
import com.swefton.backend.modules.gym.api.GymApi;
import com.swefton.backend.modules.gym.dto.CompleteGymOnboardingRequest;
import com.swefton.backend.modules.gym.dto.CompleteGymOnboardingResponse;
import com.swefton.backend.modules.gym.dto.GymResponse;
import com.swefton.backend.modules.gym.dto.UpdateGymLocationRequest;
import com.swefton.backend.modules.gym.service.GymOnboardingService;
import com.swefton.backend.modules.gym.service.GymService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('FACILITY_OWNER')")
@RequestMapping(GymApi.BASE_PATH)
public class GymController {

    private final GymService gymService;
    private final GymOnboardingService gymOnboardingService;

    @GetMapping
    public ResponseEntity<List<GymResponse>> findAllForCurrentOwner() {
        return ResponseEntity.ok(gymService.findAllForCurrentOwner());
    }

    @PostMapping
    public ResponseEntity<GymResponse> create(@Valid @RequestBody CreateFacilityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gymService.create(request));
    }

    @PostMapping("/onboarding")
    public ResponseEntity<CompleteGymOnboardingResponse> completeOnboarding(
            @Valid @RequestBody CompleteGymOnboardingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gymOnboardingService.complete(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GymResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(gymService.findById(id));
    }

    @PatchMapping("/{id}/location")
    public ResponseEntity<GymResponse> updateLocation(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGymLocationRequest request) {
        return ResponseEntity.ok(gymService.updateLocation(id, request));
    }

    @GetMapping("/facility/{facilityId}")
    public ResponseEntity<GymResponse> findByFacilityId(@PathVariable Long facilityId) {
        return ResponseEntity.ok(gymService.findByFacilityId(facilityId));
    }
}
