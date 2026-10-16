package com.swefton.backend.modules.membership.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swefton.backend.modules.membership.dto.AccessValidationResponse;
import com.swefton.backend.modules.membership.dto.GymMembershipResponse;
import com.swefton.backend.modules.membership.dto.MembershipPlanResponse;
import com.swefton.backend.modules.membership.dto.MembershipPlanRequest;
import com.swefton.backend.modules.membership.dto.RedeemAccessPassRequest;
import com.swefton.backend.modules.membership.dto.UpdateMembershipPlanRequest;
import com.swefton.backend.modules.membership.service.GymMembershipService;
import com.swefton.backend.modules.membership.service.MembershipPlanService;
import com.swefton.backend.modules.pricing.dto.PriceOfferRequest;
import com.swefton.backend.modules.pricing.dto.PriceOfferResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FACILITY_OWNER', 'ADMIN')")
@RequestMapping("/api/v1/facilities/{facilityId}")
public class FacilityMembershipController {

    private final MembershipPlanService membershipPlanService;
    private final GymMembershipService gymMembershipService;

    @GetMapping("/membership-plans")
    public ResponseEntity<List<MembershipPlanResponse>> plans(@PathVariable Long facilityId) {
        return ResponseEntity.ok(membershipPlanService.ownerPlans(facilityId));
    }

    @PostMapping("/membership-plans")
    public ResponseEntity<MembershipPlanResponse> createPlan(
            @PathVariable Long facilityId,
            @Valid @RequestBody MembershipPlanRequest request) {
        return ResponseEntity.status(201).body(membershipPlanService.create(facilityId, request));
    }

    @PutMapping("/membership-plans/{planId}")
    public ResponseEntity<MembershipPlanResponse> updatePlan(
            @PathVariable Long facilityId,
            @PathVariable Long planId,
            @Valid @RequestBody UpdateMembershipPlanRequest request) {
        return ResponseEntity.ok(membershipPlanService.update(facilityId, planId, request));
    }

    @DeleteMapping("/membership-plans/{planId}")
    public ResponseEntity<Void> deletePlan(@PathVariable Long facilityId, @PathVariable Long planId) {
        membershipPlanService.delete(facilityId, planId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/membership-plans/{planId}/offers")
    public ResponseEntity<PriceOfferResponse> createOffer(
            @PathVariable Long facilityId, @PathVariable Long planId,
            @Valid @RequestBody PriceOfferRequest request) {
        return ResponseEntity.status(201).body(membershipPlanService.createOffer(facilityId, planId, request));
    }

    @PutMapping("/membership-plans/{planId}/offers/{offerId}")
    public ResponseEntity<PriceOfferResponse> updateOffer(
            @PathVariable Long facilityId, @PathVariable Long planId, @PathVariable Long offerId,
            @Valid @RequestBody PriceOfferRequest request) {
        return ResponseEntity.ok(membershipPlanService.updateOffer(facilityId, planId, offerId, request));
    }

    @GetMapping("/membership-plans/{planId}/offers")
    public ResponseEntity<List<PriceOfferResponse>> offerHistory(@PathVariable Long facilityId, @PathVariable Long planId) {
        return ResponseEntity.ok(membershipPlanService.offerHistory(facilityId, planId));
    }

    @DeleteMapping("/membership-plans/{planId}/offers/{offerId}")
    public ResponseEntity<Void> cancelOffer(
            @PathVariable Long facilityId, @PathVariable Long planId, @PathVariable Long offerId) {
        membershipPlanService.cancelOffer(facilityId, planId, offerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/memberships")
    public ResponseEntity<List<GymMembershipResponse>> memberships(@PathVariable Long facilityId) {
        return ResponseEntity.ok(gymMembershipService.ownerMemberships(facilityId));
    }

    @PostMapping("/memberships/{membershipId}/approve")
    public ResponseEntity<GymMembershipResponse> approve(@PathVariable Long facilityId, @PathVariable Long membershipId) {
        return ResponseEntity.ok(gymMembershipService.approve(facilityId, membershipId));
    }

    @PostMapping("/memberships/{membershipId}/reject")
    public ResponseEntity<GymMembershipResponse> reject(@PathVariable Long facilityId, @PathVariable Long membershipId) {
        return ResponseEntity.ok(gymMembershipService.reject(facilityId, membershipId));
    }

    @PostMapping("/access/scan")
    public ResponseEntity<AccessValidationResponse> redeem(
            @PathVariable Long facilityId,
            @Valid @RequestBody RedeemAccessPassRequest request) {
        return ResponseEntity.ok(gymMembershipService.redeem(
                facilityId, request.token(), request.userId(), request.gymId()));
    }
}
