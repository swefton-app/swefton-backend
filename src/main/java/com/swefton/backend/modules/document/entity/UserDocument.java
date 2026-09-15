package com.swefton.backend.modules.document.entity;

import java.time.LocalDateTime;

import com.swefton.backend.modules.document.enums.DocumentType;
import com.swefton.backend.modules.document.persistence.DocumentTypeConverter;
import com.swefton.backend.modules.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_documents", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_user_documents_user_document",
                columnNames = {"user_id", "document_id"})
})
@Getter
@Setter
public class UserDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_documents_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_documents_document"))
    private Document document;

    @Convert(converter = DocumentTypeConverter.class)
    @Column(name = "type", nullable = false, length = 30)
    private DocumentType type;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column (name = "updated_at")
    private LocalDateTime updatedAt;

    @Column (name = "deleted_at")
    private LocalDateTime deletedAt;

    public UserDocument() {
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
