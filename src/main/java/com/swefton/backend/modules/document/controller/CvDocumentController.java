package com.swefton.backend.modules.document.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.swefton.backend.modules.document.api.DocumentApi;
import com.swefton.backend.modules.document.dto.DocumentResponse;
import com.swefton.backend.modules.document.dto.request.GenerateCvRequest;
import com.swefton.backend.modules.document.service.CvDocumentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('TRAINER')")
public class CvDocumentController {

    private final CvDocumentService cvDocumentService;

    @PostMapping({DocumentApi.CV_GENERATE_PATH, DocumentApi.VERSIONED_CV_GENERATE_PATH})
    public ResponseEntity<DocumentResponse> generate(@Valid @RequestBody GenerateCvRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cvDocumentService.generate(request));
    }
}
