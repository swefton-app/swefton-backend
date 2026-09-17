package com.swefton.backend.modules.document.controller;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.swefton.backend.modules.document.api.DocumentApi;
import com.swefton.backend.modules.document.dto.DocumentResponse;
import com.swefton.backend.modules.document.service.DocumentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('TRAINER')")
@RequestMapping(DocumentApi.BASE_PATH)
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("type") String type) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.upload(file, type));
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getCurrentTrainerDocuments() {
        return ResponseEntity.ok(documentService.getCurrentTrainerDocuments());
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long documentId) {
        return documentService.download(documentId);
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> delete(@PathVariable Long documentId) {
        documentService.delete(documentId);
        return ResponseEntity.noContent().build();
    }
}
