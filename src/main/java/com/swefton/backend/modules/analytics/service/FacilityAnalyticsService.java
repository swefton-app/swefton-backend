package com.swefton.backend.modules.analytics.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.time.temporal.TemporalAdjusters;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.modules.analytics.dto.AnalyticsPointResponse;
import com.swefton.backend.modules.analytics.dto.FacilityAnalyticsResponse;
import com.swefton.backend.modules.analytics.dto.HourlyCheckInPointResponse;
import com.swefton.backend.modules.analytics.dto.WeeklyCheckInResponse;
import com.swefton.backend.modules.facility.entity.Facility;
import com.swefton.backend.modules.facility.repository.FacilityRepository;
import com.swefton.backend.modules.gym.entity.Gym;
import com.swefton.backend.modules.gym.repository.GymRepository;
import com.swefton.backend.modules.machine.entity.FacilityMachine;
import com.swefton.backend.modules.machine.enums.FacilityMachineStatus;
import com.swefton.backend.modules.machine.repository.FacilityMachineRepository;
import com.swefton.backend.modules.membership.entity.GymMembership;
import com.swefton.backend.modules.membership.entity.GymVisit;
import com.swefton.backend.modules.membership.enums.MembershipStatus;
import com.swefton.backend.modules.membership.repository.GymMembershipRepository;
import com.swefton.backend.modules.membership.repository.GymVisitRepository;
import com.swefton.backend.session.ISessionUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FacilityAnalyticsService {
    private final FacilityRepository facilities;
    private final GymMembershipRepository memberships;
    private final GymVisitRepository visits;
    private final FacilityMachineRepository machines;
    private final GymRepository gyms;
    private final ISessionUser session;

    @Transactional(readOnly = true)
    public FacilityAnalyticsResponse analytics(Long facilityId, LocalDate from, LocalDate to, String requestedBucket) {
        managedFacility(facilityId);
        LocalDate rangeTo = to == null ? LocalDate.now() : to;
        LocalDate rangeFrom = from == null ? rangeTo.minusDays(29) : from;
        if (rangeFrom.isAfter(rangeTo)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before or equal to to");
        String bucket = requestedBucket == null ? "DAY" : requestedBucket.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("DAY", "MONTH", "YEAR").contains(bucket)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bucket must be DAY, MONTH or YEAR");

        LocalDateTime start = rangeFrom.atStartOfDay();
        LocalDateTime endExclusive = rangeTo.plusDays(1).atStartOfDay();
        List<GymMembership> facilityMemberships = memberships.findAllByFacilityId(facilityId);
        List<GymVisit> facilityVisits = visits.findAllByMembershipFacilityIdAndEnteredAtBetween(facilityId, start, endExclusive.minusNanos(1));
        List<FacilityMachine> facilityMachines = machines.findAllByFacilityIdOrderByMachineNameAsc(facilityId);

        List<AnalyticsPointResponse> series = new ArrayList<>();
        LocalDate pointStart = rangeFrom;
        while (!pointStart.isAfter(rangeTo)) {
            LocalDate next = switch (bucket) {
                case "MONTH" -> pointStart.plusMonths(1);
                case "YEAR" -> pointStart.plusYears(1);
                default -> pointStart.plusDays(1);
            };
            LocalDate pointEndExclusive = next.isAfter(rangeTo.plusDays(1)) ? rangeTo.plusDays(1) : next;
            LocalDateTime pointFrom = pointStart.atStartOfDay();
            LocalDateTime pointTo = pointEndExclusive.atStartOfDay();
            List<GymVisit> pointVisits = facilityVisits.stream().filter(v -> !v.getEnteredAt().isBefore(pointFrom) && v.getEnteredAt().isBefore(pointTo)).toList();
            series.add(new AnalyticsPointResponse(
                    pointStart,
                    pointEndExclusive.minusDays(1),
                    facilityMemberships.stream().filter(m -> between(m.getApprovedAt(), pointFrom, pointTo)).count(),
                    facilityMemberships.stream().filter(m -> activeAt(m, pointTo.minusNanos(1))).count(),
                    facilityMemberships.stream().filter(m -> departedBetween(m, pointFrom, pointTo)).count(),
                    pointVisits.stream().map(v -> v.getMembership().getUser().getId()).distinct().count(),
                    pointVisits.size()));
            pointStart = pointEndExclusive;
        }

        long newMembers = facilityMemberships.stream().filter(m -> between(m.getApprovedAt(), start, endExclusive)).count();
        long activeMembers = facilityMemberships.stream().filter(m -> activeAt(m, endExclusive.minusNanos(1))).count();
        long departedMembers = facilityMemberships.stream().filter(m -> departedBetween(m, start, endExclusive)).count();
        long activeAtStart = facilityMemberships.stream().filter(m -> activeAt(m, start)).count();
        long uniqueVisitors = facilityVisits.stream().map(v -> v.getMembership().getUser().getId()).distinct().count();
        long checkIns = facilityVisits.size();
        Set<Long> attendanceMembers = new HashSet<>();
        facilityVisits.forEach(v -> attendanceMembers.add(v.getMembership().getUser().getId()));
        long memberDenominator = Math.max(activeMembers, attendanceMembers.size());

        return new FacilityAnalyticsResponse(
                facilityId, rangeFrom, rangeTo, bucket,
                newMembers, activeMembers, departedMembers, uniqueVisitors, checkIns,
                round(memberDenominator == 0 ? 0 : (double) checkIns / memberDenominator),
                round(activeAtStart == 0 ? 0 : (double) departedMembers * 100 / activeAtStart),
                quantity(facilityMachines, FacilityMachineStatus.ACTIVE),
                quantity(facilityMachines, FacilityMachineStatus.NEEDS_REPAIR, FacilityMachineStatus.UNDER_REPAIR),
                quantity(facilityMachines, FacilityMachineStatus.OUT_OF_SERVICE),
                series);
    }

    @Transactional(readOnly = true)
    public WeeklyCheckInResponse hourlyCheckIns(Long facilityId, LocalDate requestedWeekStart) {
        managedFacility(facilityId);
        Gym gym = gyms.findByFacilityId(facilityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gym not found"));
        LocalDate selectedDate = requestedWeekStart == null ? LocalDate.now() : requestedWeekStart;
        LocalDate weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        List<GymVisit> weekVisits = visits.findAllByMembershipFacilityIdAndEnteredAtBetween(
                facilityId, weekStart.atStartOfDay(), weekEnd.plusDays(2).atStartOfDay().minusNanos(1));
        List<HourlyCheckInPointResponse> hours = new ArrayList<>();

        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            LocalDate date = weekStart.plusDays(dayOffset);
            boolean weekend = date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY
                    || date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY;
            LocalTime opensAt = gym.isOpen24Hours() ? LocalTime.MIDNIGHT
                    : weekend ? gym.getWeekendOpenTime() : gym.getWeekdayOpenTime();
            LocalTime closesAt = gym.isOpen24Hours() ? LocalTime.MIDNIGHT
                    : weekend ? gym.getWeekendCloseTime() : gym.getWeekdayCloseTime();
            if (opensAt == null || closesAt == null) continue;

            LocalDateTime opening = date.atTime(opensAt);
            LocalDateTime closing = gym.isOpen24Hours() || !closesAt.isAfter(opensAt)
                    ? date.plusDays(1).atTime(closesAt)
                    : date.atTime(closesAt);
            for (LocalDateTime hourStart = opening; hourStart.isBefore(closing); hourStart = hourStart.plusHours(1)) {
                LocalDateTime hourEnd = hourStart.plusHours(1).isAfter(closing) ? closing : hourStart.plusHours(1);
                LocalDateTime slotStart = hourStart;
                long checkIns = weekVisits.stream()
                        .filter(visit -> !visit.getEnteredAt().isBefore(slotStart) && visit.getEnteredAt().isBefore(hourEnd))
                        .count();
                hours.add(new HourlyCheckInPointResponse(
                        date, date.getDayOfWeek().name(), hourStart, hourEnd, checkIns));
            }
        }

        return new WeeklyCheckInResponse(
                facilityId,
                weekStart,
                weekEnd,
                hours.stream().mapToLong(HourlyCheckInPointResponse::checkIns).sum(),
                hours);
    }

    private Facility managedFacility(Long id) {
        Facility facility = facilities.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facility not found"));
        if (!"ADMIN".equals(session.getRole()) && !facility.getOwner().getId().equals(session.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot view analytics for this facility");
        }
        return facility;
    }

    private boolean activeAt(GymMembership membership, LocalDateTime instant) {
        if (membership.getApprovedAt() == null || membership.getApprovedAt().isAfter(instant)) return false;
        if (MembershipStatus.ACTIVE.equals(membership.getStatus())) {
            return membership.getEndsOn() == null || instant.isBefore(membership.getEndsOn().plusDays(1).atStartOfDay());
        }
        return (MembershipStatus.CANCELLED.equals(membership.getStatus()) || MembershipStatus.EXPIRED.equals(membership.getStatus()))
                && departureAt(membership) != null && departureAt(membership).isAfter(instant);
    }

    private boolean departedBetween(GymMembership membership, LocalDateTime from, LocalDateTime to) {
        return between(departureAt(membership), from, to);
    }

    private LocalDateTime departureAt(GymMembership membership) {
        if (MembershipStatus.CANCELLED.equals(membership.getStatus())) {
            return membership.getCancelledAt() != null ? membership.getCancelledAt() : membership.getStatusChangedAt();
        }
        if (MembershipStatus.EXPIRED.equals(membership.getStatus())) {
            return membership.getStatusChangedAt() != null ? membership.getStatusChangedAt()
                    : membership.getEndsOn() == null ? null : membership.getEndsOn().plusDays(1).atStartOfDay();
        }
        if (MembershipStatus.ACTIVE.equals(membership.getStatus()) && membership.getEndsOn() != null
                && membership.getEndsOn().isBefore(LocalDate.now())) {
            return membership.getEndsOn().plusDays(1).atStartOfDay();
        }
        return null;
    }

    private boolean between(LocalDateTime value, LocalDateTime from, LocalDateTime to) {
        return value != null && !value.isBefore(from) && value.isBefore(to);
    }

    private long quantity(List<FacilityMachine> all, FacilityMachineStatus... statuses) {
        Set<FacilityMachineStatus> accepted = Set.of(statuses);
        return all.stream().filter(m -> accepted.contains(m.getStatus())).mapToLong(FacilityMachine::getQuantity).sum();
    }

    private double round(double value) { return Math.round(value * 100.0) / 100.0; }
}
