package com.swefton.backend.modules.document.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CvLanguageRequest {

    @NotBlank
    @Size(max = 80)
    private String language;

    @NotBlank
    private String level;
}
