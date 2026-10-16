package com.swefton.backend.modules.post.dto;
import java.time.LocalDateTime;
import java.util.List;
public record PostResponse(Long id, String authorType, Long authorId, String authorName, String authorImageUrl, Long facilityId, String text,
        List<PostMediaResponse> media, long likeCount, long commentCount, boolean likedByMe, boolean manageableByMe,
        LocalDateTime createdAt, LocalDateTime updatedAt) {}
