package com.swefton.backend.modules.document.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swefton.backend.modules.document.dto.DocumentResponse;
import com.swefton.backend.modules.document.dto.request.GenerateCvRequest;
import com.swefton.backend.modules.document.service.CvDocumentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('STAFF')")
@RequestMapping("/api/v1/staff/documents/cv")
public class StaffCvDocumentController {

    private final CvDocumentService cvDocumentService;

    @PostMapping("/generate")
    public ResponseEntity<DocumentResponse> generate(@Valid @RequestBody GenerateCvRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cvDocumentService.generate(request));
    }
}
