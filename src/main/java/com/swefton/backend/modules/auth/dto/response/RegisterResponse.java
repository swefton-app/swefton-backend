package com.swefton.backend.modules.auth.dto.response;

import com.swefton.backend.modules.user.enums.RoleCode;

public record RegisterResponse(
        Long id,
        String email,
        RoleCode role,
        boolean emailConfirmed) {
}
