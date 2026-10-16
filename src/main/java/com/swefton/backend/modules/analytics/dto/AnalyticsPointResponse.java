package com.swefton.backend.modules.analytics.dto;

import java.time.LocalDate;

public record AnalyticsPointResponse(
        LocalDate periodStart,
        LocalDate periodEnd,
        long newMembers,
        long activeMembers,
        long departedMembers,
        long uniqueVisitors,
        long checkIns) {}
