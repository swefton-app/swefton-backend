package com.swefton.backend.modules.video.dto;

import java.time.LocalDateTime;

import com.swefton.backend.modules.video.api.VideoApi;
import com.swefton.backend.modules.video.entity.Video;

public record VideoResponse(
        Long id,
        String originalName,
        String contentType,
        Long fileSize,
        String url,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public VideoResponse(Video video) {
        this(
                video.getId(),
                video.getOriginalName(),
                video.getContentType(),
                video.getFileSize(),
                VideoApi.BASE_PATH + "/" + video.getId(),
                video.getCreatedAt(),
                video.getUpdatedAt());
    }
}
