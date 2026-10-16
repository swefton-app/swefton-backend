package com.swefton.backend.modules.document.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CvExperienceRequest {

    @NotBlank
    @Size(max = 120)
    private String company;

    @NotBlank
    @Size(max = 120)
    private String position;

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "must use YYYY-MM")
    private String startDate;

    @Pattern(regexp = "^$|\\d{4}-\\d{2}", message = "must use YYYY-MM")
    private String endDate;

    private boolean current;

    @Size(max = 1_500)
    private String description;
}
