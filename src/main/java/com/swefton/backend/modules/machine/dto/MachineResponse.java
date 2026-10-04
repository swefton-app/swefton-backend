package com.swefton.backend.modules.machine.dto;

import java.util.List;

import com.swefton.backend.modules.image.dto.ImagePojo;

public record MachineResponse(
        Long id,
        String name,
        String code,
        String description,
        String facilityCategory,
        List<MachineMovementResponse> movements,
        List<ImagePojo> images) {
}
