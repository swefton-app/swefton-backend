package com.swefton.backend.modules.activity.dto;
import java.time.DayOfWeek;
import java.time.LocalTime;
import jakarta.validation.constraints.NotNull;
public record ActivityScheduleRequest(@NotNull DayOfWeek dayOfWeek,@NotNull LocalTime startTime,@NotNull LocalTime endTime){}
