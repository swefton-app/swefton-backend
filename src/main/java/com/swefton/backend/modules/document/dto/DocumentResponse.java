package com.swefton.backend.modules.document.dto;

import java.time.LocalDateTime;

import com.swefton.backend.modules.document.enums.DocumentType;

public record DocumentResponse(
        Long id,
        String fileName,
        String contentType,
        DocumentType type,
        long size,
        LocalDateTime uploadedAt) {
}
