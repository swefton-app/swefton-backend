package com.swefton.backend.modules.facility.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateStaffInvitationRequest(@NotBlank @Email String email) {
}
