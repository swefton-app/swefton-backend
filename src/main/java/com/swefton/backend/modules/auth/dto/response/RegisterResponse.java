package com.swefton.backend.modules.auth.dto.response;

public record RegisterResponse(
        Long id,
        String email,
        String role,
        boolean emailConfirmed) {
}
