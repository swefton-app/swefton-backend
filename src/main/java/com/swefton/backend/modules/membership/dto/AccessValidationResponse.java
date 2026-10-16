package com.swefton.backend.modules.membership.dto;

import java.time.LocalDateTime;

public record AccessValidationResponse(
        boolean approved,
        String direction,
        String memberName,
        String gymName,
        LocalDateTime processedAt,
        Integer remainingEntriesThisWeek) {
}
