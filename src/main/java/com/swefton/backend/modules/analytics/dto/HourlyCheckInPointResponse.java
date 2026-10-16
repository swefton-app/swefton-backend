package com.swefton.backend.modules.analytics.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HourlyCheckInPointResponse(
        LocalDate date,
        String dayOfWeek,
        LocalDateTime hourStart,
        LocalDateTime hourEnd,
        long checkIns) {}
