package com.swefton.backend.infrastructure.web.response;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class FileResponseHelper {

    public ResponseEntity<Resource> download(
            Resource resource,
            String fileName,
            String contentType,
            long contentLength) {

        return ResponseEntity.ok()
                .contentType(parseMediaType(contentType))
                .contentLength(contentLength)
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment(fileName))
                .body(resource);
    }

    public ResponseEntity<Resource> inline(
            Resource resource,
            String fileName,
            String contentType,
            long contentLength) {

        return ResponseEntity.ok()
                .contentType(parseMediaType(contentType))
                .contentLength(contentLength)
                .header(HttpHeaders.CONTENT_DISPOSITION, inlineDisposition(fileName))
                .body(resource);
    }

    private MediaType parseMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String attachment(String fileName) {
        return ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString();
    }

    private String inlineDisposition(String fileName) {
        return ContentDisposition.inline()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString();
    }
}
