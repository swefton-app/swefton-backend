package com.swefton.backend.modules.auth.dto.request;

import com.swefton.backend.modules.user.enums.RoleCode;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleAuthRequest{

    @NotBlank
    private String credential;

    private RoleCode role;
}