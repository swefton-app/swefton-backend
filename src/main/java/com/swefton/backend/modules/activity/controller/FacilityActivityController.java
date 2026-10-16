package com.swefton.backend.modules.activity.controller;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.swefton.backend.modules.activity.dto.*;
import com.swefton.backend.modules.activity.service.FacilityActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
@RestController @RequiredArgsConstructor @PreAuthorize("hasAnyRole('FACILITY_OWNER','ADMIN')") @RequestMapping("/api/v1/facilities/{facilityId}/activities")
public class FacilityActivityController{
 private final FacilityActivityService service;
 @GetMapping public List<FacilityActivityResponse> list(@PathVariable Long facilityId){return service.list(facilityId);}
 @PostMapping public ResponseEntity<FacilityActivityResponse> create(@PathVariable Long facilityId,@Valid @RequestBody FacilityActivityRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(facilityId,request));}
 @PutMapping("/{activityId}") public FacilityActivityResponse update(@PathVariable Long facilityId,@PathVariable Long activityId,@Valid @RequestBody FacilityActivityRequest request){return service.update(facilityId,activityId,request);}
 @DeleteMapping("/{activityId}") public ResponseEntity<Void> delete(@PathVariable Long facilityId,@PathVariable Long activityId){service.delete(facilityId,activityId);return ResponseEntity.noContent().build();}
}
