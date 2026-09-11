package com.swefton.backend.modules.auth.provider.google;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GoogleIdentity{

    private String subject;
    private String email;
    private boolean emailVerified;

    private String firstName;
    private String lastName;
    private String pictureUrl;
}