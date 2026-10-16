package com.swefton.backend.modules.membership.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.modules.facility.entity.Facility;
import com.swefton.backend.modules.facility.repository.FacilityRepository;
import com.swefton.backend.modules.membership.dto.MembershipPlanResponse;
import com.swefton.backend.modules.membership.dto.MembershipPlanRequest;
import com.swefton.backend.modules.membership.dto.UpdateMembershipPlanRequest;
import com.swefton.backend.modules.membership.entity.MembershipPlan;
import com.swefton.backend.modules.membership.repository.MembershipPlanRepository;
import com.swefton.backend.modules.pricing.dto.PriceOfferRequest;
import com.swefton.backend.modules.pricing.dto.PriceOfferResponse;
import com.swefton.backend.modules.pricing.service.PriceOfferService;
import com.swefton.backend.modules.user.enums.RoleCode;
import com.swefton.backend.session.ISessionUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MembershipPlanService {

    private final MembershipPlanRepository membershipPlanRepository;
    private final FacilityRepository facilityRepository;
    private final ISessionUser sessionUser;
    private final PriceOfferService priceOfferService;

    @Transactional
    public List<MembershipPlan> activePlans(Facility facility) {
        return membershipPlanRepository
                .findAllByFacilityIdAndDeletedAtIsNullAndActiveTrueOrderByDurationDaysAscVisitsPerWeekAsc(facility.getId());
    }

    @Transactional
    public List<MembershipPlanResponse> ownerPlans(Long facilityId) {
        managedFacility(facilityId);
        return membershipPlanRepository.findAllByFacilityIdAndDeletedAtIsNullOrderByDurationDaysAscVisitsPerWeekAsc(facilityId)
                .stream().map(this::toOwnerResponse).toList();
    }

    @Transactional
    public MembershipPlanResponse create(Long facilityId, MembershipPlanRequest request) {
        Facility facility = managedFacility(facilityId);
        MembershipPlan plan = new MembershipPlan();
        plan.setFacility(facility);
        apply(plan, request.visitsPerWeek(), request.durationMonths(), request.price(),
                request.currency(), request.active());
        return toOwnerResponse(membershipPlanRepository.saveAndFlush(plan));
    }

    @Transactional
    public MembershipPlanResponse update(Long facilityId, Long planId, UpdateMembershipPlanRequest request) {
        managedFacility(facilityId);
        MembershipPlan plan = membershipPlanRepository.findByIdAndFacilityIdAndDeletedAtIsNull(planId, facilityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership plan not found"));
        apply(plan, request.visitsPerWeek(), request.durationMonths(), request.price(),
                request.currency(), request.active());
        return toOwnerResponse(membershipPlanRepository.saveAndFlush(plan));
    }

    @Transactional
    public void delete(Long facilityId, Long planId) {
        managedFacility(facilityId);
        MembershipPlan plan = plan(facilityId, planId);
        plan.setActive(false);
        plan.setDeletedAt(LocalDateTime.now());
        membershipPlanRepository.save(plan);
    }

    @Transactional
    public PriceOfferResponse createOffer(Long facilityId, Long planId, PriceOfferRequest request) {
        managedFacility(facilityId);
        MembershipPlan plan = plan(facilityId, planId);
        return priceOfferService.create(PriceOfferService.MEMBERSHIP_PLAN, planId, plan.getPrice(), request);
    }

    @Transactional
    public PriceOfferResponse updateOffer(Long facilityId, Long planId, Long offerId, PriceOfferRequest request) {
        managedFacility(facilityId);
        MembershipPlan plan = plan(facilityId, planId);
        return priceOfferService.update(PriceOfferService.MEMBERSHIP_PLAN, planId, offerId, plan.getPrice(), request);
    }

    @Transactional(readOnly = true)
    public List<PriceOfferResponse> offerHistory(Long facilityId, Long planId) {
        managedFacility(facilityId);
        plan(facilityId, planId);
        return priceOfferService.history(PriceOfferService.MEMBERSHIP_PLAN, planId);
    }

    @Transactional
    public void cancelOffer(Long facilityId, Long planId, Long offerId) {
        managedFacility(facilityId);
        plan(facilityId, planId);
        priceOfferService.cancel(PriceOfferService.MEMBERSHIP_PLAN, planId, offerId);
    }

    public MembershipPlanResponse toResponse(MembershipPlan plan) {
        PriceOfferResponse offer = priceOfferService.activeResponse(PriceOfferService.MEMBERSHIP_PLAN, plan.getId());
        return response(plan, offer);
    }

    private MembershipPlanResponse toOwnerResponse(MembershipPlan plan) {
        PriceOfferResponse offer = priceOfferService.activeOrScheduledResponse(PriceOfferService.MEMBERSHIP_PLAN, plan.getId());
        return response(plan, offer);
    }

    private MembershipPlanResponse response(MembershipPlan plan, PriceOfferResponse offer) {
        return new MembershipPlanResponse(plan.getId(), planName(plan.getVisitsPerWeek(), durationMonths(plan)), plan.getVisitsPerWeek(), plan.getPrice(),
                offer == null || !offer.active() ? plan.getPrice() : offer.salePrice(), plan.getCurrency(), plan.getDurationDays(),
                durationMonths(plan), plan.isActive(), offer);
    }

    private MembershipPlan plan(Long facilityId, Long planId) {
        return membershipPlanRepository.findByIdAndFacilityIdAndDeletedAtIsNull(planId, facilityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership plan not found"));
    }

    private void apply(MembershipPlan plan, Integer visitsPerWeek, Integer durationMonths,
            java.math.BigDecimal price, String currency, boolean active) {
        plan.setName(planName(visitsPerWeek, durationMonths));
        plan.setVisitsPerWeek(visitsPerWeek);
        plan.setDurationDays(durationMonths * 30);
        plan.setPrice(price);
        plan.setCurrency(currency.trim().toUpperCase(Locale.ROOT));
        plan.setActive(active);
    }

    private int durationMonths(MembershipPlan plan) {
        return Math.max(1, (plan.getDurationDays() + 29) / 30);
    }

    private String planName(Integer visitsPerWeek, Integer durationMonths) {
        String monthLabel = durationMonths == 1 ? "month" : "months";
        return durationMonths + " " + monthLabel + " · " + visitsPerWeek + " days/week";
    }

    private Facility managedFacility(Long facilityId) {
        Facility facility = facilityRepository.findByIdAndDeletedAtIsNull(facilityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facility not found"));
        if (!facility.getOwner().getId().equals(sessionUser.getUserId()) && !sessionUser.hasRole(RoleCode.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot manage this facility");
        }
        return facility;
    }
}
