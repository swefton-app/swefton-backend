package com.swefton.backend.modules.activity.dto;
import java.util.List;
public record FacilityActivityResponse(Long id,String name,String description,String category,Integer capacity,
 Long responsibleStaffId,String responsibleName,String status,List<ActivityScheduleResponse> schedules){}
