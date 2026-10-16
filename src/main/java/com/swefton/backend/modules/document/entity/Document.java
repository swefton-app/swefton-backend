package com.swefton.backend.modules.document.entity;

import java.time.LocalDateTime;

import com.swefton.backend.modules.document.persistence.DocumentTypeConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "documents", uniqueConstraints = {
        @UniqueConstraint(name = "uk_documents_storage_key", columnNames = "storage_key")
})
@Getter
@Setter
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Convert(converter = DocumentTypeConverter.class)
    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Document() {
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
