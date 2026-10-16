package com.swefton.backend.modules.membership.dto;

import java.time.LocalTime;

public record OpeningHoursResponse(
        String days,
        LocalTime opensAt,
        LocalTime closesAt,
        boolean open24Hours) {
}
