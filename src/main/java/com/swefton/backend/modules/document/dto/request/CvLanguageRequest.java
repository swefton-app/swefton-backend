package com.swefton.backend.modules.document.dto.request;

import com.swefton.backend.modules.document.enums.CvLanguageLevel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CvLanguageRequest {

    @NotBlank
    @Size(max = 80)
    private String language;

    @NotNull
    private CvLanguageLevel level;
}
