package com.fitplatform.backend.modules.image.controller;

import com.fitplatform.backend.modules.image.api.ImageApi;
import com.fitplatform.backend.modules.image.dto.ImageRequest;
import com.fitplatform.backend.modules.image.dto.ImageResponse;
import com.fitplatform.backend.modules.image.service.ImageService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ImageApi.BASE_PATH)
public class ImageController {

    private final ImageService service;

    public ImageController(
            ImageService service
    ) {
        this.service = service;
    }

    @GetMapping
    public List<ImageResponse> findAll() {

        return service.findAll();
    }

    @GetMapping("/{id}")
    public ImageResponse findById(
            @PathVariable Long id
    ) {

        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ImageResponse create(
            @RequestBody ImageRequest request
    ) {

        return service.create(request);
    }

    @PutMapping("/{id}")
    public ImageResponse update(
            @PathVariable Long id,
            @RequestBody ImageRequest request
    ) {

        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id
    ) {

        service.delete(id);
    }
}

