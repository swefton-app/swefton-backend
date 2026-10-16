package com.swefton.backend.modules.gym.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateGymImageRequest(
        @NotNull Long imageId) {
}
