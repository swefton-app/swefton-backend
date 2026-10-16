package com.swefton.backend.modules.membership.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAccessPassRequest(@NotBlank String direction) {
}
