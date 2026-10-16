package com.swefton.backend.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "Code must contain 6 digits") String code,
        @NotBlank @Size(min = 8, max = 100) String newPassword,
        @NotBlank String confirmPassword) {
}
