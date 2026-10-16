package com.swefton.backend.modules.post.dto;
import java.util.List;
import jakarta.validation.constraints.Size;
public record PostRequest(@Size(max=2000) String text, Long facilityId,
        @Size(max=10) List<Long> imageIds, @Size(max=10) List<Long> videoIds) {}
