package com.swefton.backend.modules.image.dto;

import java.time.LocalDateTime;

import com.swefton.backend.modules.image.entity.Image;
import com.swefton.backend.modules.image.enums.ImageType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImagePojo {

    private Long id;
    private Long userId;
    private ImageType type;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private Integer position;
    private String url;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ImagePojo() {
    }

    public ImagePojo(Image entity) {
        this.id = entity.getId();
        this.userId = entity.getUser() != null ? entity.getUser().getId() : null;
        this.type = entity.getType();
        this.originalName = entity.getOriginalName();
        this.contentType = entity.getContentType();
        this.fileSize = entity.getFileSize();
        this.position = entity.getPosition();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
        this.url = "/api/v1/images/" + entity.getId();
    }
}
