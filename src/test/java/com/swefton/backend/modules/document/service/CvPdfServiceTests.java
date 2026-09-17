package com.swefton.backend.modules.document.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import com.swefton.backend.modules.document.dto.request.CvCertificationRequest;
import com.swefton.backend.modules.document.dto.request.CvEducationRequest;
import com.swefton.backend.modules.document.dto.request.CvExperienceRequest;
import com.swefton.backend.modules.document.dto.request.CvLanguageRequest;
import com.swefton.backend.modules.document.dto.request.GenerateCvRequest;
import com.swefton.backend.modules.document.enums.CvLanguageLevel;
import com.swefton.backend.modules.document.enums.CvTemplate;

class CvPdfServiceTests {

    private CvPdfService cvPdfService;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        cvPdfService = new CvPdfService(templateEngine);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            CvTemplate.MODERN,
            CvTemplate.PROFESSIONAL,
            CvTemplate.MINIMAL
    })
    void generatesAValidPdfForEveryTemplate(String template) {
        GenerateCvRequest request = new GenerateCvRequest();
        request.setTemplate(template);
        request.setFirstName("Franko");
        request.setLastName("Kaloshi");
        request.setProfessionalTitle("Personal Trainer");
        request.setEmail("franko@swefton.com");
        request.setCity("Tirana");
        request.setSummary("Strength and mobility coach.");
        request.setSkills(java.util.List.of("Strength Training", "Client Assessment"));

        CvExperienceRequest experience = new CvExperienceRequest();
        experience.setPosition("Personal Trainer");
        experience.setCompany("Power Gym");
        experience.setStartDate("2024-01");
        experience.setCurrent(true);
        experience.setDescription("Built tailored strength programs.");
        request.setExperiences(java.util.List.of(experience));

        CvEducationRequest education = new CvEducationRequest();
        education.setInstitution("Sports Academy");
        education.setDegree("BSc");
        education.setFieldOfStudy("Sports Science");
        education.setStartDate("2019-09");
        education.setEndDate("2022-06");
        request.setEducation(java.util.List.of(education));

        CvCertificationRequest certification = new CvCertificationRequest();
        certification.setName("Certified Personal Trainer");
        certification.setIssuer("NASM");
        certification.setIssueDate("2023-03");
        request.setCertifications(java.util.List.of(certification));

        CvLanguageRequest language = new CvLanguageRequest();
        language.setLanguage("Albanian");
        language.setLevel(CvLanguageLevel.NATIVE);
        request.setLanguages(java.util.List.of(language));

        byte[] pdf = cvPdfService.generate(request);

        assertThat(pdf.length).isGreaterThan(1_000);
        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }
}
