package com.swefton.backend.modules.facility.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swefton.backend.modules.facility.dto.SetStaffPasswordRequest;
import com.swefton.backend.modules.facility.dto.StaffAccountSetupResponse;
import com.swefton.backend.modules.facility.dto.StaffInvitationDetailsResponse;
import com.swefton.backend.modules.facility.service.FacilityStaffInvitationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/staff-invitations")
public class PublicStaffInvitationController {

    private final FacilityStaffInvitationService invitationService;

    @GetMapping("/{token}")
    public ResponseEntity<StaffInvitationDetailsResponse> details(@PathVariable String token) {
        return ResponseEntity.ok(invitationService.details(token));
    }

    @PostMapping("/{token}/password")
    public ResponseEntity<StaffAccountSetupResponse> setPassword(
            @PathVariable String token,
            @Valid @RequestBody SetStaffPasswordRequest request) {
        return ResponseEntity.ok(invitationService.setPassword(token, request));
    }
}
