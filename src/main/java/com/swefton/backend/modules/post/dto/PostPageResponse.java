package com.swefton.backend.modules.post.dto;
import java.time.LocalDateTime;
import java.util.List;
public record PostPageResponse(List<PostResponse> items, LocalDateTime nextCreatedAt, Long nextId) {}
