package com.swefton.backend.modules.document.dto;

import java.time.LocalDateTime;

public record DocumentResponse(
        Long id,
        String fileName,
        String contentType,
        String type,
        long size,
        LocalDateTime uploadedAt) {
}
