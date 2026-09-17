package com.swefton.backend.modules.document.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.swefton.backend.modules.document.entity.UserDocument;

public interface UserDocumentRepository extends JpaRepository<UserDocument, Long> {

    @EntityGraph(attributePaths = "document")
    List<UserDocument> findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = "document")
    Optional<UserDocument> findByUserIdAndDocumentIdAndDeletedAtIsNull(Long userId, Long documentId);

    @EntityGraph(attributePaths = "document")
    Optional<UserDocument> findByUserIdAndTypeAndDeletedAtIsNull(Long userId, String type);
}
