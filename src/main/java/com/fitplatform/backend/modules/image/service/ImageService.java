package com.fitplatform.backend.modules.image.service;

import com.fitplatform.backend.modules.image.dto.ImageRequest;
import com.fitplatform.backend.modules.image.dto.ImageResponse;

import java.util.List;

public interface ImageService {

    List<ImageResponse> findAll();

    ImageResponse findById(Long id);

    ImageResponse create(
            ImageRequest request
    );

    ImageResponse update(
            Long id,
            ImageRequest request
    );

    void delete(Long id);
}
