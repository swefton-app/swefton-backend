package com.swefton.backend.modules.document.service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.swefton.backend.modules.document.dto.request.GenerateCvRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CvPdfService {

    private final TemplateEngine templateEngine;

    public byte[] generate(GenerateCvRequest request) {
        normalizeCollections(request);

        Context context = new Context();
        context.setVariable("cv", request);

        String template = switch (request.getTemplate()) {
            case MODERN -> "cv/modern";
            case PROFESSIONAL -> "cv/professional";
            case MINIMAL -> "cv/minimal";
        };
        String html = templateEngine.process(template, context);

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not generate CV PDF", exception);
        }
    }

    private void normalizeCollections(GenerateCvRequest request) {
        if (request.getExperiences() == null) {
            request.setExperiences(new ArrayList<>());
        }
        if (request.getEducation() == null) {
            request.setEducation(new ArrayList<>());
        }
        if (request.getCertifications() == null) {
            request.setCertifications(new ArrayList<>());
        }
        if (request.getSkills() == null) {
            request.setSkills(new ArrayList<>());
        }
        if (request.getLanguages() == null) {
            request.setLanguages(new ArrayList<>());
        }
    }
}
