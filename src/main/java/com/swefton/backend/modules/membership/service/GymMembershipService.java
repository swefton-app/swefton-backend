package com.swefton.backend.modules.membership.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.modules.facility.entity.Facility;
import com.swefton.backend.modules.facility.repository.FacilityRepository;
import com.swefton.backend.modules.gym.repository.GymRepository;
import com.swefton.backend.modules.membership.dto.AccessPassResponse;
import com.swefton.backend.modules.membership.dto.AccessValidationResponse;
import com.swefton.backend.modules.membership.dto.CreateMembershipRequest;
import com.swefton.backend.modules.membership.dto.GymMembershipResponse;
import com.swefton.backend.modules.membership.dto.MembershipPlanResponse;
import com.swefton.backend.modules.membership.entity.GymAccessPass;
import com.swefton.backend.modules.membership.entity.GymMembership;
import com.swefton.backend.modules.membership.entity.GymVisit;
import com.swefton.backend.modules.membership.entity.MembershipPlan;
import com.swefton.backend.modules.membership.enums.AccessDirection;
import com.swefton.backend.modules.membership.enums.MembershipStatus;
import com.swefton.backend.modules.membership.repository.GymAccessPassRepository;
import com.swefton.backend.modules.membership.repository.GymMembershipRepository;
import com.swefton.backend.modules.membership.repository.GymVisitRepository;
import com.swefton.backend.modules.membership.repository.MembershipPlanRepository;
import com.swefton.backend.modules.payment.dto.PaymentResponse;
import com.swefton.backend.modules.payment.entity.Payment;
import com.swefton.backend.modules.payment.service.DevelopmentPaymentService;
import com.swefton.backend.modules.pricing.service.PriceOfferService;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.entity.UserProfile;
import com.swefton.backend.modules.user.enums.RoleCode;
import com.swefton.backend.modules.user.repository.UserProfileRepository;
import com.swefton.backend.modules.user.repository.UserRepository;
import com.swefton.backend.session.ISessionUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GymMembershipService {

    private final MembershipPlanRepository membershipPlanRepository;
    private final GymMembershipRepository gymMembershipRepository;
    private final GymAccessPassRepository gymAccessPassRepository;
    private final GymVisitRepository gymVisitRepository;
    private final FacilityRepository facilityRepository;
    private final GymRepository gymRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ISessionUser sessionUser;
    private final DevelopmentPaymentService paymentService;
    private final PriceOfferService priceOfferService;
    private final MembershipPlanService membershipPlanService;

    @Transactional
    public GymMembershipResponse request(CreateMembershipRequest request) {
        MembershipPlan plan = membershipPlanRepository.findById(request.planId())
                .filter(item -> item.isActive() && item.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership plan not found"));
        Long userId = sessionUser.getUserId();
        if (gymMembershipRepository.existsByUserIdAndFacilityIdAndStatusIn(
                userId, plan.getFacility().getId(), List.of(MembershipStatus.PENDING, MembershipStatus.ACTIVE))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have a membership or pending request for this gym");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        GymMembership membership = new GymMembership();
        membership.setUser(user);
        membership.setFacility(plan.getFacility());
        membership.setPlan(plan);
        Payment payment = paymentService.charge(user, PriceOfferService.MEMBERSHIP_PLAN, plan.getId(), plan.getPrice(),
                priceOfferService.effectivePrice(PriceOfferService.MEMBERSHIP_PLAN, plan.getId(), plan.getPrice()),
                plan.getCurrency(), request.payment());
        membership.setPayment(payment);
        return toResponse(gymMembershipRepository.saveAndFlush(membership));
    }

    @Transactional
    public List<GymMembershipResponse> mine() {
        return gymMembershipRepository.findAllByUserIdOrderByRequestedAtDesc(sessionUser.getUserId())
                .stream().map(this::refreshAndRespond).toList();
    }

    @Transactional
    public List<GymMembershipResponse> ownerMemberships(Long facilityId) {
        managedFacility(facilityId);
        return gymMembershipRepository.findAllByFacilityIdOrderByRequestedAtDesc(facilityId)
                .stream().map(this::refreshAndRespond).toList();
    }

    @Transactional
    public GymMembershipResponse approve(Long facilityId, Long membershipId) {
        managedFacility(facilityId);
        GymMembership membership = membership(facilityId, membershipId);
        if (!MembershipStatus.PENDING.equals(membership.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending memberships can be approved");
        }
        LocalDate today = LocalDate.now();
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setStatusChangedAt(LocalDateTime.now());
        membership.setApprovedAt(LocalDateTime.now());
        membership.setStartsOn(today);
        membership.setEndsOn(today.plusMonths(Math.max(1, (membership.getPlan().getDurationDays() + 29) / 30)).minusDays(1));
        membership.setWeekStartedOn(startOfWeek(today));
        membership.setEntriesThisWeek(0);
        return toResponse(gymMembershipRepository.saveAndFlush(membership));
    }

    @Transactional
    public GymMembershipResponse reject(Long facilityId, Long membershipId) {
        managedFacility(facilityId);
        GymMembership membership = membership(facilityId, membershipId);
        if (!MembershipStatus.PENDING.equals(membership.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending memberships can be rejected");
        }
        membership.setStatus(MembershipStatus.REJECTED);
        membership.setStatusChangedAt(LocalDateTime.now());
        return toResponse(gymMembershipRepository.saveAndFlush(membership));
    }

    @Transactional
    public AccessPassResponse createPass(Long membershipId, String requestedDirection) {
        AccessDirection direction;
        try {
            direction = AccessDirection.from(requestedDirection);
        } catch (RuntimeException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Direction must be ENTER or EXIT");
        }
        GymMembership membership = gymMembershipRepository.findByIdAndUserId(membershipId, sessionUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership not found"));
        refreshMembership(membership);
        requireActive(membership);
        resetWeeklyCount(membership);
        if (direction == AccessDirection.ENTER) {
            if (membership.isCheckedIn()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "You are already checked in. Generate an exit pass instead");
            }
            Integer remaining = remainingEntries(membership);
            if (remaining != null && remaining <= 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Your weekly gym entries have been used");
            }
        } else if (!membership.isCheckedIn()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You are not currently checked in");
        }

        GymAccessPass pass = new GymAccessPass();
        pass.setMembership(membership);
        pass.setDirection(direction);
        pass.setToken(UUID.randomUUID().toString().replace("-", ""));
        pass.setExpiresAt(LocalDateTime.now().plusMinutes(2));
        gymMembershipRepository.save(membership);
        gymAccessPassRepository.saveAndFlush(pass);
        Long gymId = gymId(membership.getFacility().getId());
        String qrPayload = "swefton://gym-access?token=" + pass.getToken()
                + "&userId=" + membership.getUser().getId()
                + "&gymId=" + gymId
                + "&facilityId=" + membership.getFacility().getId()
                + "&direction=" + direction.name();
        return new AccessPassResponse(
                pass.getToken(),
                qrPayload,
                membership.getUser().getId(),
                gymId,
                membership.getFacility().getId(),
                direction.name(),
                pass.getExpiresAt(),
                remainingEntries(membership));
    }

    @Transactional
    public AccessValidationResponse redeem(Long facilityId, String token, Long claimedUserId, Long claimedGymId) {
        managedFacility(facilityId);
        GymAccessPass pass = gymAccessPassRepository.findByToken(token.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Access pass not found"));
        GymMembership membership = pass.getMembership();
        if (!membership.getFacility().getId().equals(facilityId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This pass belongs to another gym");
        }
        Long actualGymId = gymId(facilityId);
        if (!membership.getUser().getId().equals(claimedUserId) || !actualGymId.equals(claimedGymId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "QR identity does not match this membership");
        }
        if (pass.getUsedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This access pass has already been used");
        }
        if (pass.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "This access pass has expired");
        }
        refreshMembership(membership);
        requireActive(membership);
        resetWeeklyCount(membership);
        LocalDateTime now = LocalDateTime.now();
        if (pass.getDirection() == AccessDirection.ENTER) {
            if (membership.isCheckedIn()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Member is already checked in");
            }
            Integer remaining = remainingEntries(membership);
            if (remaining != null && remaining <= 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Weekly entry allowance has been used");
            }
            membership.setEntriesThisWeek(membership.getEntriesThisWeek() + 1);
            membership.setCheckedIn(true);
            GymVisit visit = new GymVisit();
            visit.setMembership(membership);
            visit.setEnteredAt(now);
            gymVisitRepository.save(visit);
        } else {
            if (!membership.isCheckedIn()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Member is not checked in");
            }
            GymVisit visit = gymVisitRepository.findFirstByMembershipIdAndExitedAtIsNullOrderByEnteredAtDesc(membership.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Open gym visit not found"));
            visit.setExitedAt(now);
            gymVisitRepository.save(visit);
            membership.setCheckedIn(false);
        }
        pass.setUsedAt(now);
        gymMembershipRepository.save(membership);
        gymAccessPassRepository.save(pass);
        return new AccessValidationResponse(
                true,
                pass.getDirection().name(),
                memberName(membership.getUser()),
                membership.getFacility().getName(),
                now,
                remainingEntries(membership));
    }

    @Transactional
    public GymMembershipResponse currentFor(Long facilityId) {
        return gymMembershipRepository.findFirstByUserIdAndFacilityIdOrderByRequestedAtDesc(sessionUser.getUserId(), facilityId)
                .map(this::refreshAndRespond)
                .orElse(null);
    }

    @Transactional
    public GymMembershipResponse cancel(Long membershipId) {
        GymMembership membership = gymMembershipRepository.findByIdAndUserId(membershipId, sessionUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership not found"));
        if (!MembershipStatus.ACTIVE.equals(membership.getStatus()) && !MembershipStatus.PENDING.equals(membership.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only active or pending memberships can be cancelled");
        }
        LocalDateTime now = LocalDateTime.now();
        gymVisitRepository.findFirstByMembershipIdAndExitedAtIsNullOrderByEnteredAtDesc(membershipId).ifPresent(visit -> {
            visit.setExitedAt(now);
            gymVisitRepository.save(visit);
        });
        membership.setCheckedIn(false);
        membership.setStatus(MembershipStatus.CANCELLED);
        membership.setCancelledAt(now);
        membership.setStatusChangedAt(now);
        return toResponse(gymMembershipRepository.saveAndFlush(membership));
    }

    private GymMembershipResponse refreshAndRespond(GymMembership membership) {
        refreshMembership(membership);
        resetWeeklyCount(membership);
        return toResponse(membership);
    }

    private void refreshMembership(GymMembership membership) {
        if (MembershipStatus.ACTIVE.equals(membership.getStatus())
                && membership.getEndsOn() != null
                && membership.getEndsOn().isBefore(LocalDate.now())) {
            membership.setStatus(MembershipStatus.EXPIRED);
            membership.setStatusChangedAt(LocalDateTime.now());
            membership.setCheckedIn(false);
            gymMembershipRepository.save(membership);
        }
    }

    private void resetWeeklyCount(GymMembership membership) {
        if (!MembershipStatus.ACTIVE.equals(membership.getStatus())) return;
        LocalDate weekStart = startOfWeek(LocalDate.now());
        if (!weekStart.equals(membership.getWeekStartedOn())) {
            membership.setWeekStartedOn(weekStart);
            membership.setEntriesThisWeek(0);
            gymMembershipRepository.save(membership);
        }
    }

    private LocalDate startOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private Integer remainingEntries(GymMembership membership) {
        Integer allowed = membership.getPlan().getVisitsPerWeek();
        return allowed == null ? null : Math.max(allowed - membership.getEntriesThisWeek(), 0);
    }

    private GymMembershipResponse toResponse(GymMembership membership) {
        return new GymMembershipResponse(
                membership.getId(),
                membership.getFacility().getId(),
                gymId(membership.getFacility().getId()),
                membership.getFacility().getName(),
                memberName(membership.getUser()),
                membership.getUser().getEmail(),
                membership.getStatus(),
                membershipPlanService.toResponse(membership.getPlan()),
                membership.getPayment() == null ? null : new PaymentResponse(membership.getPayment()),
                membership.getRequestedAt(),
                membership.getStartsOn(),
                membership.getEndsOn(),
                membership.getEntriesThisWeek(),
                remainingEntries(membership),
                membership.isCheckedIn());
    }

    private String memberName(User user) {
        return userProfileRepository.findByUserId(user.getId())
                .map(this::profileName)
                .orElse(user.getEmail());
    }

    private String profileName(UserProfile profile) {
        if (profile.getDisplayName() != null && !profile.getDisplayName().isBlank()) return profile.getDisplayName();
        return (profile.getFirstName() + " " + profile.getLastName()).trim();
    }

    private void requireActive(GymMembership membership) {
        if (!MembershipStatus.ACTIVE.equals(membership.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An active gym membership is required");
        }
    }

    private GymMembership membership(Long facilityId, Long membershipId) {
        return gymMembershipRepository.findByIdAndFacilityId(membershipId, facilityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership not found"));
    }

    private Long gymId(Long facilityId) {
        return gymRepository.findByFacilityId(facilityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gym not found"))
                .getId();
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
