package com.swefton.backend.modules.analytics.dto;

import java.time.LocalDate;
import java.util.List;

public record WeeklyCheckInResponse(
        Long facilityId,
        LocalDate weekStart,
        LocalDate weekEnd,
        long totalCheckIns,
        List<HourlyCheckInPointResponse> hours) {}
