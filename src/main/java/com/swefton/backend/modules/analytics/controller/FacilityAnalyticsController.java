package com.swefton.backend.modules.analytics.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.swefton.backend.modules.analytics.dto.FacilityAnalyticsResponse;
import com.swefton.backend.modules.analytics.dto.WeeklyCheckInResponse;
import com.swefton.backend.modules.analytics.service.FacilityAnalyticsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FACILITY_OWNER', 'ADMIN')")
@RequestMapping("/api/v1/facilities/{facilityId}/analytics")
public class FacilityAnalyticsController {
    private final FacilityAnalyticsService analytics;

    @GetMapping
    public FacilityAnalyticsResponse get(
            @PathVariable Long facilityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") String bucket) {
        return analytics.analytics(facilityId, from, to, bucket);
    }

    @GetMapping("/check-ins/hourly")
    public WeeklyCheckInResponse hourlyCheckIns(
            @PathVariable Long facilityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return analytics.hourlyCheckIns(facilityId, weekStart);
    }
}
