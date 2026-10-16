package com.swefton.backend.modules.membership.dto;

import java.time.LocalDateTime;

public record AccessPassResponse(
        String token,
        String qrPayload,
        Long userId,
        Long gymId,
        Long facilityId,
        String direction,
        LocalDateTime expiresAt,
        Integer remainingEntriesThisWeek) {
}
