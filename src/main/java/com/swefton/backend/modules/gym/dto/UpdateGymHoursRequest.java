package com.swefton.backend.modules.gym.dto;

import java.time.LocalTime;

public record UpdateGymHoursRequest(
        boolean open24Hours,
        LocalTime weekdayOpenTime,
        LocalTime weekdayCloseTime,
        LocalTime weekendOpenTime,
        LocalTime weekendCloseTime) {
}
