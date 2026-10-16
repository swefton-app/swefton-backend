package com.swefton.backend.modules.activity.service;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.swefton.backend.modules.activity.dto.*;
import com.swefton.backend.modules.activity.entity.*;
import com.swefton.backend.modules.activity.repository.FacilityActivityRepository;
import com.swefton.backend.modules.facility.entity.*;
import com.swefton.backend.modules.facility.repository.*;
import com.swefton.backend.modules.user.repository.UserProfileRepository;
import com.swefton.backend.session.ISessionUser;
import lombok.RequiredArgsConstructor;

@Service @RequiredArgsConstructor
public class FacilityActivityService {
 private final FacilityActivityRepository activities; private final FacilityRepository facilities;
 private final FacilityStaffRepository staff; private final UserProfileRepository profiles; private final ISessionUser session;
 @Transactional(readOnly=true) public List<FacilityActivityResponse> list(Long facilityId){managed(facilityId);return activities.findAllByFacilityIdAndDeletedAtIsNullOrderByNameAsc(facilityId).stream().map(this::response).toList();}
 @Transactional public FacilityActivityResponse create(Long facilityId,FacilityActivityRequest request){FacilityActivity a=new FacilityActivity();a.setFacility(managed(facilityId));apply(a,request);return response(activities.saveAndFlush(a));}
 @Transactional public FacilityActivityResponse update(Long facilityId,Long id,FacilityActivityRequest request){managed(facilityId);FacilityActivity a=find(facilityId,id);apply(a,request);return response(activities.saveAndFlush(a));}
 @Transactional public void delete(Long facilityId,Long id){managed(facilityId);FacilityActivity a=find(facilityId,id);a.setDeletedAt(LocalDateTime.now());activities.saveAndFlush(a);}
 private void apply(FacilityActivity a,FacilityActivityRequest r){String status=r.status().trim().toUpperCase(Locale.ROOT);if(!Set.of("DRAFT","ACTIVE","INACTIVE").contains(status))throw bad("Activity status must be DRAFT, ACTIVE or INACTIVE");FacilityStaff responsible=r.responsibleStaffId()==null?null:staff.findById(r.responsibleStaffId()).filter(s->s.getFacility().getId().equals(a.getFacility().getId())&&s.isActive()).orElseThrow(()->bad("Responsible staff member is invalid"));for(ActivityScheduleRequest s:r.schedules())if(!s.startTime().isBefore(s.endTime()))throw bad("Activity start time must be before end time");checkOwnOverlaps(r.schedules());checkStaffOverlap(a.getId(),responsible,r.schedules());a.setName(r.name().trim());a.setDescription(r.description()==null?null:r.description().trim());a.setCategory(r.category().trim());a.setCapacity(r.capacity());a.setResponsibleStaff(responsible);a.setStatus(status);a.getSchedules().clear();for(ActivityScheduleRequest s:r.schedules()){ActivitySchedule schedule=new ActivitySchedule();schedule.setActivity(a);schedule.setDayOfWeek(s.dayOfWeek());schedule.setStartTime(s.startTime());schedule.setEndTime(s.endTime());a.getSchedules().add(schedule);}}
 private void checkOwnOverlaps(List<ActivityScheduleRequest> schedules){for(int i=0;i<schedules.size();i++)for(int j=i+1;j<schedules.size();j++){var a=schedules.get(i);var b=schedules.get(j);if(a.dayOfWeek()==b.dayOfWeek()&&overlaps(a.startTime(),a.endTime(),b.startTime(),b.endTime()))throw bad("Activity schedules overlap on "+a.dayOfWeek());}}
 private void checkStaffOverlap(Long currentId,FacilityStaff responsible,List<ActivityScheduleRequest> requested){if(responsible==null)return;for(FacilityActivity other:activities.findAllByResponsibleStaffIdAndDeletedAtIsNull(responsible.getId())){if(Objects.equals(currentId,other.getId())||"INACTIVE".equals(other.getStatus()))continue;for(ActivitySchedule existing:other.getSchedules())for(ActivityScheduleRequest candidate:requested)if(existing.getDayOfWeek()==candidate.dayOfWeek()&&overlaps(existing.getStartTime(),existing.getEndTime(),candidate.startTime(),candidate.endTime()))throw new ResponseStatusException(HttpStatus.CONFLICT,"This staff member already has an overlapping activity on "+candidate.dayOfWeek());}}
 private boolean overlaps(java.time.LocalTime a1,java.time.LocalTime a2,java.time.LocalTime b1,java.time.LocalTime b2){return a1.isBefore(b2)&&b1.isBefore(a2);}
 private Facility managed(Long id){Facility f=facilities.findByIdAndDeletedAtIsNull(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Facility not found"));if(!"ADMIN".equals(session.getRole())&&!f.getOwner().getId().equals(session.getUserId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You cannot manage this facility");return f;}
 private FacilityActivity find(Long facilityId,Long id){return activities.findByIdAndFacilityIdAndDeletedAtIsNull(id,facilityId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Activity not found"));}
 private FacilityActivityResponse response(FacilityActivity a){String name=null;if(a.getResponsibleStaff()!=null){var user=a.getResponsibleStaff().getUser();var p=profiles.findByUserId(user.getId()).orElse(null);name=p==null?user.getEmail():(p.getDisplayName()!=null&&!p.getDisplayName().isBlank()?p.getDisplayName():p.getFirstName()+" "+p.getLastName());}return new FacilityActivityResponse(a.getId(),a.getName(),a.getDescription(),a.getCategory(),a.getCapacity(),a.getResponsibleStaff()==null?null:a.getResponsibleStaff().getId(),name,a.getStatus(),a.getSchedules().stream().map(s->new ActivityScheduleResponse(s.getId(),s.getDayOfWeek(),s.getStartTime(),s.getEndTime())).toList());}
 private ResponseStatusException bad(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}
}
