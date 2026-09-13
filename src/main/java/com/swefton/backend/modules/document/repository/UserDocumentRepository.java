package com.swefton.backend.modules.document.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.swefton.backend.modules.document.entity.UserDocument;

public interface UserDocumentRepository extends JpaRepository<UserDocument, Long> {

    @EntityGraph(attributePaths = "document")
    List<UserDocument> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = "document")
    Optional<UserDocument> findByUserIdAndDocumentId(Long userId, Long documentId);
}
