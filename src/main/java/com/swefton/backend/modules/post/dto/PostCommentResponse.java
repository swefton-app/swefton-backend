package com.swefton.backend.modules.post.dto;
import java.time.LocalDateTime;
public record PostCommentResponse(Long id, Long userId, String authorName, String text, LocalDateTime createdAt, LocalDateTime updatedAt) {}
