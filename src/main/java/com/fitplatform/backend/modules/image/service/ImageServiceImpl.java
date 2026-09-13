package com.fitplatform.backend.modules.image.service;

import com.fitplatform.backend.modules.image.dto.ImageRequest;
import com.fitplatform.backend.modules.image.dto.ImageResponse;
import com.fitplatform.backend.modules.image.repository.ImageRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImageServiceImpl
        implements ImageService {

    private final ImageRepository repository;

    public ImageServiceImpl(
            ImageRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public List<ImageResponse> findAll() {

        return repository.findAll()
                .stream()
                .map(entity ->
                        new ImageResponse(
                                entity.getId()
                        )
                )
                .toList();
    }

    @Override
    public ImageResponse findById(Long id) {

        var entity = repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Image not found with id: " + id
                        )
                );

        return new ImageResponse(
                entity.getId()
        );
    }

    @Override
    public ImageResponse create(
            ImageRequest request
    ) {

        throw new UnsupportedOperationException(
                "Create not implemented yet"
        );
    }

    @Override
    public ImageResponse update(
            Long id,
            ImageRequest request
    ) {

        throw new UnsupportedOperationException(
                "Update not implemented yet"
        );
    }

    @Override
    public void delete(Long id) {

        repository.deleteById(id);
    }
}
