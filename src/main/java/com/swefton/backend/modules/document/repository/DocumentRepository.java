package com.swefton.backend.modules.document.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swefton.backend.modules.document.entity.Document;

public interface DocumentRepository extends JpaRepository<Document, Long> {
}
