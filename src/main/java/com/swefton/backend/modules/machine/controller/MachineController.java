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

import com.swefton.backend.modules.machine.api.MachineApi;
import com.swefton.backend.modules.machine.dto.MachineRequest;
import com.swefton.backend.modules.machine.dto.MachineResponse;
import com.swefton.backend.modules.machine.service.MachineService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping(
        path = MachineApi.BASE_PATH,
        produces = MediaType.APPLICATION_JSON_VALUE)
public class MachineController {

    private final MachineService machineService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACILITY_OWNER')")
    public ResponseEntity<List<MachineResponse>> findAll() {
        return ResponseEntity.ok(machineService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACILITY_OWNER')")
    public ResponseEntity<MachineResponse> findById(
            @PathVariable @Positive Long id) {
        return ResponseEntity.ok(machineService.findById(id));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MachineResponse> create(
            @Valid @RequestBody MachineRequest request) {
        MachineResponse created = machineService.create(request);
        URI location = URI.create(MachineApi.BASE_PATH + "/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MachineResponse> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody MachineRequest request) {
        return ResponseEntity.ok(machineService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable @Positive Long id) {
        machineService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
