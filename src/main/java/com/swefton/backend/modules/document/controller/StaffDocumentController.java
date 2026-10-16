package com.swefton.backend.modules.document.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.swefton.backend.modules.document.dto.DocumentResponse;
import com.swefton.backend.modules.document.service.DocumentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('STAFF')")
@RequestMapping("/api/v1/staff/documents")
public class StaffDocumentController {

    private final DocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("type") String type) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.upload(file, type));
    }
}
