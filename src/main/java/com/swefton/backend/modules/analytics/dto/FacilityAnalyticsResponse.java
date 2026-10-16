package com.swefton.backend.modules.analytics.dto;

import java.time.LocalDate;
import java.util.List;

public record FacilityAnalyticsResponse(
        Long facilityId,
        LocalDate from,
        LocalDate to,
        String bucket,
        long newMembers,
        long activeMembers,
        long departedMembers,
        long uniqueVisitors,
        long checkIns,
        double averageAttendancePerMember,
        double churnRate,
        long activeMachines,
        long machinesUnderRepair,
        long machinesOutOfService,
        List<AnalyticsPointResponse> series) {}
