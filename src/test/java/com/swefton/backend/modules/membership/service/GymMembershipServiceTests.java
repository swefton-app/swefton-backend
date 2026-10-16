package com.swefton.backend.modules.membership.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.swefton.backend.modules.facility.entity.Facility;
import com.swefton.backend.modules.facility.repository.FacilityRepository;
import com.swefton.backend.modules.gym.entity.Gym;
import com.swefton.backend.modules.gym.repository.GymRepository;
import com.swefton.backend.modules.membership.dto.AccessPassResponse;
import com.swefton.backend.modules.membership.dto.AccessValidationResponse;
import com.swefton.backend.modules.membership.dto.GymMembershipResponse;
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
import com.swefton.backend.modules.payment.service.DevelopmentPaymentService;
import com.swefton.backend.modules.pricing.service.PriceOfferService;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.repository.UserProfileRepository;
import com.swefton.backend.modules.user.repository.UserRepository;
import com.swefton.backend.session.ISessionUser;

@ExtendWith(MockitoExtension.class)
class GymMembershipServiceTests {

    @Mock MembershipPlanRepository planRepository;
    @Mock GymMembershipRepository membershipRepository;
    @Mock GymAccessPassRepository passRepository;
    @Mock GymVisitRepository visitRepository;
    @Mock FacilityRepository facilityRepository;
    @Mock GymRepository gymRepository;
    @Mock UserRepository userRepository;
    @Mock UserProfileRepository profileRepository;
    @Mock ISessionUser sessionUser;
    @Mock DevelopmentPaymentService paymentService;
    @Mock PriceOfferService priceOfferService;
    @Mock MembershipPlanService membershipPlanService;

    private GymMembershipService service;
    private User member;
    private Facility facility;
    private MembershipPlan plan;
    private GymMembership membership;

    @BeforeEach
    void setUp() {
        service = new GymMembershipService(
                planRepository, membershipRepository, passRepository, visitRepository,
                facilityRepository, gymRepository, userRepository, profileRepository, sessionUser,
                paymentService, priceOfferService, membershipPlanService);
        member = new User();
        member.setId(11L);
        member.setEmail("member@swefton.test");
        User owner = new User();
        owner.setId(22L);
        facility = new Facility();
        facility.setId(33L);
        facility.setName("Swefton Gym");
        facility.setOwner(owner);
        plan = new MembershipPlan();
        plan.setId(44L);
        plan.setFacility(facility);
        plan.setName("3 days a week");
        plan.setVisitsPerWeek(3);
        plan.setPrice(new BigDecimal("29.99"));
        plan.setDurationDays(30);
        membership = new GymMembership();
        membership.setId(55L);
        membership.setUser(member);
        membership.setFacility(facility);
        membership.setPlan(plan);
        membership.setRequestedAt(LocalDateTime.now().minusDays(1));
        Gym gym = new Gym();
        gym.setId(66L);
        gym.setFacility(facility);
        when(gymRepository.findByFacilityId(33L)).thenReturn(Optional.of(gym));
    }

    @Test
    void ownerApprovalActivatesMembershipForThePlanDuration() {
        when(sessionUser.getUserId()).thenReturn(22L);
        when(facilityRepository.findByIdAndDeletedAtIsNull(33L)).thenReturn(Optional.of(facility));
        when(membershipRepository.findByIdAndFacilityId(55L, 33L)).thenReturn(Optional.of(membership));
        when(membershipRepository.saveAndFlush(any(GymMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GymMembershipResponse response = service.approve(33L, 55L);

        assertThat(response.status()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(response.startsOn()).isEqualTo(LocalDate.now());
        assertThat(response.endsOn()).isEqualTo(LocalDate.now().plusDays(29));
        assertThat(response.remainingEntriesThisWeek()).isEqualTo(3);
    }

    @Test
    void memberCanCreateShortLivedEntryPassWhenWeeklyEntriesRemain() {
        activateMembership(2);
        when(sessionUser.getUserId()).thenReturn(11L);
        when(membershipRepository.findByIdAndUserId(55L, 11L)).thenReturn(Optional.of(membership));
        when(passRepository.saveAndFlush(any(GymAccessPass.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccessPassResponse response = service.createPass(55L, "ENTER");

        assertThat(response.direction()).isEqualTo("ENTER");
        assertThat(response.remainingEntriesThisWeek()).isEqualTo(1);
        assertThat(response.qrPayload())
                .startsWith("swefton://gym-access?token=")
                .contains("userId=11", "gymId=66", "facilityId=33");
        assertThat(response.userId()).isEqualTo(11L);
        assertThat(response.gymId()).isEqualTo(66L);
        assertThat(response.expiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void redeemingEntryPassConsumesOneWeeklyEntryAndStartsVisit() {
        activateMembership(1);
        GymAccessPass pass = new GymAccessPass();
        pass.setMembership(membership);
        pass.setDirection(AccessDirection.ENTER);
        pass.setToken("valid-token");
        pass.setExpiresAt(LocalDateTime.now().plusMinutes(1));
        when(sessionUser.getUserId()).thenReturn(22L);
        when(facilityRepository.findByIdAndDeletedAtIsNull(33L)).thenReturn(Optional.of(facility));
        when(passRepository.findByToken("valid-token")).thenReturn(Optional.of(pass));

        AccessValidationResponse response = service.redeem(33L, "valid-token", 11L, 66L);

        assertThat(response.approved()).isTrue();
        assertThat(response.remainingEntriesThisWeek()).isEqualTo(1);
        assertThat(membership.isCheckedIn()).isTrue();
        assertThat(membership.getEntriesThisWeek()).isEqualTo(2);
        assertThat(pass.getUsedAt()).isNotNull();
        verify(visitRepository).save(any(GymVisit.class));
    }

    private void activateMembership(int entriesThisWeek) {
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setStartsOn(LocalDate.now().minusDays(2));
        membership.setEndsOn(LocalDate.now().plusDays(20));
        membership.setWeekStartedOn(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
        membership.setEntriesThisWeek(entriesThisWeek);
    }
}
