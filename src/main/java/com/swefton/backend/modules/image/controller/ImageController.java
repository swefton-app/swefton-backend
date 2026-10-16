package com.swefton.backend.modules.image.controller;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.swefton.backend.modules.image.api.ImageApi;
import com.swefton.backend.modules.image.dto.ImagePojo;
import com.swefton.backend.modules.image.service.ImageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(ImageApi.BASE_PATH)
public class ImageController {

    private final ImageService imageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImagePojo> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart("image") ImagePojo request) {
        return ResponseEntity.status(HttpStatus.CREATED)

                .body(imageService.upload(file, request));
    }

    @GetMapping
    public ResponseEntity<List<ImagePojo>> findCurrentUserImages() {
        return ResponseEntity.ok(imageService.findCurrentUserImages());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> getContent(@PathVariable Long id) {
        return imageService.getContent(id);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ImagePojo> update(
            @PathVariable Long id,
            @RequestBody ImagePojo request) {
        return ResponseEntity.ok(imageService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        imageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
