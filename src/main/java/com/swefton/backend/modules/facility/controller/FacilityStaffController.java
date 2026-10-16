package com.swefton.backend.modules.facility.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swefton.backend.modules.facility.api.FacilityApi;
import com.swefton.backend.modules.facility.dto.FacilityStaffRoleResponse;
import com.swefton.backend.modules.facility.dto.CreateStaffInvitationRequest;
import com.swefton.backend.modules.facility.dto.FacilityStaffInvitationResponse;
import com.swefton.backend.modules.facility.service.FacilityStaffService;
import com.swefton.backend.modules.facility.service.FacilityStaffInvitationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FACILITY_OWNER', 'ADMIN')")
@RequestMapping(FacilityApi.BASE_PATH)
public class FacilityStaffController {

    private final FacilityStaffService facilityStaffService;
    private final FacilityStaffInvitationService invitationService;

    @GetMapping("/staff-roles")
    public ResponseEntity<List<FacilityStaffRoleResponse>> findAvailableRoles(
            @RequestParam String category) {
        return ResponseEntity.ok(facilityStaffService.findAvailableRoles(category));
    }

    @PostMapping("/{facilityId}/staff-invitations")
    public ResponseEntity<FacilityStaffInvitationResponse> invite(
            @PathVariable Long facilityId,
            @Valid @RequestBody CreateStaffInvitationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invitationService.invite(facilityId, request));
    }

    @GetMapping("/{facilityId}/staff-invitations")
    public ResponseEntity<List<FacilityStaffInvitationResponse>> listInvitations(
            @PathVariable Long facilityId) {
        return ResponseEntity.ok(invitationService.list(facilityId));
    }

    @DeleteMapping("/{facilityId}/staff-invitations/{invitationId}")
    public ResponseEntity<Void> revokeInvitation(
            @PathVariable Long facilityId,
            @PathVariable Long invitationId) {
        invitationService.revoke(facilityId, invitationId);
        return ResponseEntity.noContent().build();
    }
}
