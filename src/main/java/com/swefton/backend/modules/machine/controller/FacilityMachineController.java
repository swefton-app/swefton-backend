package com.swefton.backend.modules.machine.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swefton.backend.modules.facility.api.FacilityApi;
import com.swefton.backend.modules.machine.dto.CreateFacilityMachineRequest;
import com.swefton.backend.modules.machine.dto.FacilityMachineResponse;
import com.swefton.backend.modules.machine.dto.UpdateFacilityMachineRequest;
import com.swefton.backend.modules.machine.service.FacilityMachineService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FACILITY_OWNER', 'ADMIN')")
@RequestMapping(
        path = FacilityApi.BASE_PATH + "/{facilityId}/machines",
        produces = MediaType.APPLICATION_JSON_VALUE)
public class FacilityMachineController {

    private final FacilityMachineService facilityMachineService;

    @GetMapping
    public ResponseEntity<List<FacilityMachineResponse>> findAll(
            @PathVariable @Positive Long facilityId) {
        return ResponseEntity.ok(facilityMachineService.findAll(facilityId));
    }

    @GetMapping("/{facilityMachineId}")
    public ResponseEntity<FacilityMachineResponse> findById(
            @PathVariable @Positive Long facilityId,
            @PathVariable @Positive Long facilityMachineId) {
        return ResponseEntity.ok(facilityMachineService.findById(facilityId, facilityMachineId));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FacilityMachineResponse> create(
            @PathVariable @Positive Long facilityId,
            @Valid @RequestBody CreateFacilityMachineRequest request) {
        FacilityMachineResponse created = facilityMachineService.create(facilityId, request);
        URI location = URI.create(
                FacilityApi.BASE_PATH + "/" + facilityId + "/machines/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping(path = "/{facilityMachineId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FacilityMachineResponse> update(
            @PathVariable @Positive Long facilityId,
            @PathVariable @Positive Long facilityMachineId,
            @Valid @RequestBody UpdateFacilityMachineRequest request) {
        return ResponseEntity.ok(
                facilityMachineService.update(facilityId, facilityMachineId, request));
    }

    @DeleteMapping("/{facilityMachineId}")
    public ResponseEntity<Void> delete(
            @PathVariable @Positive Long facilityId,
            @PathVariable @Positive Long facilityMachineId) {
        facilityMachineService.delete(facilityId, facilityMachineId);
        return ResponseEntity.noContent().build();
    }
}
