package com.swefton.backend.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleAuthRequest{

    @NotBlank
    private String credential;

    private String role;
}
