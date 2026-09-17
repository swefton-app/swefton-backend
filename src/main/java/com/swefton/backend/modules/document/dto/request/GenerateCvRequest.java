package com.swefton.backend.modules.document.dto.request;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerateCvRequest {

    @NotBlank
    private String template;

    @NotBlank
    @Size(max = 80)
    private String firstName;

    @NotBlank
    @Size(max = 80)
    private String lastName;

    @NotBlank
    @Size(max = 120)
    private String professionalTitle;

    @NotBlank
    @Email
    @Size(max = 254)
    private String email;

    @Size(max = 40)
    private String phone;

    @Size(max = 120)
    private String city;

    @Size(max = 2_000)
    private String summary;

    @Valid
    @Size(max = 20)
    private List<CvExperienceRequest> experiences = new ArrayList<>();

    @Valid
    @Size(max = 20)
    private List<CvEducationRequest> education = new ArrayList<>();

    @Valid
    @Size(max = 30)
    private List<CvCertificationRequest> certifications = new ArrayList<>();

    @Size(max = 40)
    private List<@NotBlank @Size(max = 80) String> skills = new ArrayList<>();

    @Valid
    @Size(max = 20)
    private List<CvLanguageRequest> languages = new ArrayList<>();
}
