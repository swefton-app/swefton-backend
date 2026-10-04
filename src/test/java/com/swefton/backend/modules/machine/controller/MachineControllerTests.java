package com.swefton.backend.modules.machine.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.swefton.backend.modules.machine.dto.MachineRequest;
import com.swefton.backend.modules.machine.dto.MachineResponse;
import com.swefton.backend.modules.machine.dto.MachineMovementRequest;
import com.swefton.backend.modules.machine.dto.MachineMovementResponse;
import com.swefton.backend.modules.machine.enums.MachineMuscleGroup;
import com.swefton.backend.modules.machine.service.MachineService;

@ExtendWith(MockitoExtension.class)
class MachineControllerTests {

    @Mock
    private MachineService machineService;

    private MachineController controller;

    @BeforeEach
    void setUp() {
        controller = new MachineController(machineService);
    }

    @Test
    void findAllReturnsMachines() {
        MachineResponse machine = response(5L, "PRESS-01");
        when(machineService.findAll()).thenReturn(List.of(machine));

        ResponseEntity<List<MachineResponse>> result = controller.findAll();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).containsExactly(machine);
    }

    @Test
    void findByIdReturnsMachine() {
        MachineResponse machine = response(5L, "PRESS-01");
        when(machineService.findById(5L)).thenReturn(machine);

        ResponseEntity<MachineResponse> result = controller.findById(5L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(machine);
    }

    @Test
    void createReturnsCreatedMachineAndLocation() {
        MachineRequest request = request("PRESS-01");
        MachineResponse machine = response(5L, "PRESS-01");
        when(machineService.create(request)).thenReturn(machine);

        ResponseEntity<MachineResponse> result = controller.create(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getHeaders().getLocation()).isEqualTo(URI.create("/api/v1/machine/5"));
        assertThat(result.getBody()).isSameAs(machine);
    }

    @Test
    void updateReturnsUpdatedMachine() {
        MachineRequest request = request("PRESS-02");
        MachineResponse machine = response(5L, "PRESS-02");
        when(machineService.update(5L, request)).thenReturn(machine);

        ResponseEntity<MachineResponse> result = controller.update(5L, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(machine);
    }

    @Test
    void deleteReturnsNoContent() {
        ResponseEntity<Void> result = controller.delete(5L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.getBody()).isNull();
        verify(machineService).delete(5L);
    }

    private MachineRequest request(String code) {
        return new MachineRequest(
                "Chest Press",
                code,
                "Upper-body machine",
                "GYM",
                List.of(new MachineMovementRequest(
                        null,
                        "Chest Press",
                        MachineMuscleGroup.CHEST,
                        30L,
                        "Keep your back against the pad",
                        0)),
                List.of(9L));
    }

    private MachineResponse response(Long id, String code) {
        return new MachineResponse(
                id,
                "Chest Press",
                code,
                "Upper-body machine",
                "Gym",
                List.of(new MachineMovementResponse(
                        20L,
                        "Chest Press",
                        "Chest",
                        30L,
                        "/api/v1/videos/30",
                        "chest-press.mp4",
                        "Keep your back against the pad",
                        0)),
                List.of());
    }
}
