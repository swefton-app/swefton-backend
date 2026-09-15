package com.swefton.backend.modules.document.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CvCertificationRequest {

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 150)
    private String issuer;

    @Pattern(regexp = "^$|\\d{4}-\\d{2}", message = "must use YYYY-MM")
    private String issueDate;

    @Pattern(regexp = "^$|\\d{4}-\\d{2}", message = "must use YYYY-MM")
    private String expirationDate;

    @Size(max = 150)
    private String credentialId;
}
