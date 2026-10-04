package com.swefton.backend.modules.machine.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import com.swefton.backend.modules.machine.entity.FacilityMachine;
import com.swefton.backend.modules.machine.entity.MachineMovement;

public record FacilityMachineResponse(
        Long id,
        Long facilityId,
        Long machineId,
        String machineName,
        String machineCode,
        String facilityCategory,
        List<MachineMovementResponse> movements,
        Integer quantity,
        String status,
        String notes,
        LocalDate expectedArrivalDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public FacilityMachineResponse(FacilityMachine entity) {
        this(
                entity.getId(),
                entity.getFacility().getId(),
                entity.getMachine().getId(),
                entity.getMachine().getName(),
                entity.getMachine().getCode(),
                humanize(entity.getMachine().getFacilityCategory()),
                entity.getMachine().getMovements().stream()
                        .sorted(Comparator.comparing(MachineMovement::getPosition))
                        .map(MachineMovementResponse::new)
                        .toList(),
                entity.getQuantity(),
                entity.getStatus().getLabel(),
                entity.getNotes(),
                entity.getExpectedArrivalDate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private static String humanize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String[] words = value.toLowerCase(java.util.Locale.ROOT).split("_");
        StringBuilder label = new StringBuilder();
        for (String word : words) {
            if (!label.isEmpty()) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return label.toString();
    }
}
