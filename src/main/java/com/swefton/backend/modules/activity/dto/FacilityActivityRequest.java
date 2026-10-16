package com.swefton.backend.modules.activity.dto;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
public record FacilityActivityRequest(@NotBlank @Size(max=150) String name,@Size(max=2000) String description,
 @NotBlank @Size(max=80) String category,@NotNull @Min(1) Integer capacity,Long responsibleStaffId,
 @NotBlank String status,@NotNull List<@Valid ActivityScheduleRequest> schedules){}
