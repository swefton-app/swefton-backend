package com.swefton.backend.modules.activity.dto;
import java.time.DayOfWeek;
import java.time.LocalTime;
public record ActivityScheduleResponse(Long id,DayOfWeek dayOfWeek,LocalTime startTime,LocalTime endTime){}
