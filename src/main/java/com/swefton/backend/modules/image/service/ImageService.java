package com.swefton.backend.modules.image.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.infrastructure.storage.ObjectStorage;
import com.swefton.backend.infrastructure.web.response.FileResponseHelper;
import com.swefton.backend.modules.image.dto.ImagePojo;
import com.swefton.backend.modules.image.entity.Image;
import com.swefton.backend.modules.image.repository.ImageRepository;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.repository.UserRepository;
import com.swefton.backend.session.ISessionUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp");

    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final ObjectStorage objectStorage;
    private final FileResponseHelper fileResponseHelper;
    private final ISessionUser sessionUser;

    @Value("${app.images.max-file-size-bytes:10485760}")
    private long maxFileSizeBytes;

    @Transactional
    public ImagePojo upload(MultipartFile file, ImagePojo request) {
        validateUpload(file, request);
        User user = currentUser();
        String originalName = safeOriginalName(file.getOriginalFilename());
        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        String storageKey = createStorageKey(user.getId(), originalName);

        try (InputStream content = file.getInputStream()) {
            long storedSize = objectStorage.store(storageKey, content);
            try {
                Image image = new Image();
                image.setUser(user);
                image.setType(request.getType());
                image.setFilePath(storageKey);
                image.setOriginalName(originalName);
                image.setContentType(contentType);
                image.setFileSize(storedSize);
                image.setPosition(request.getPosition());
                return new ImagePojo(imageRepository.saveAndFlush(image));
            } catch (RuntimeException exception) {
                objectStorage.delete(storageKey);
                throw exception;
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not read uploaded image",
                    exception);
        }
    }

    @Transactional(readOnly = true)
    public List<ImagePojo> findCurrentUserImages() {
        return imageRepository
                .findAllByUserIdAndDeletedAtIsNullOrderByPositionAscCreatedAtDesc(sessionUser.getUserId())
                .stream()
                .map(ImagePojo::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> getContent(Long id) {
        Image image = imageRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));
        Resource resource = objectStorage.load(image.getFilePath());
        return fileResponseHelper.inline(
                resource,
                image.getOriginalName(),
                image.getContentType(),
                image.getFileSize());
    }

    @Transactional
    public ImagePojo update(Long id, ImagePojo request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image data is required");
        }
        Image image = ownedImage(id);
        if (request.getType() != null) {
            image.setType(request.getType());
        }
        if (request.getPosition() != null) {
            if (request.getPosition() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image position must not be negative");
            }
            image.setPosition(request.getPosition());
        }
        return new ImagePojo(imageRepository.saveAndFlush(image));
    }

    @Transactional
    public void delete(Long id) {
        Image image = ownedImage(id);
        image.setDeletedAt(LocalDateTime.now());
        imageRepository.saveAndFlush(image);
        objectStorage.delete(image.getFilePath());
    }

    private User currentUser() {
        return userRepository.findById(sessionUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Image ownedImage(Long id) {
        return imageRepository.findByIdAndUserIdAndDeletedAtIsNull(id, sessionUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found"));
    }

    private void validateUpload(MultipartFile file, ImagePojo request) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image must not be empty");
        }
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image data is required");
        }
        if (request.getType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image type is required");
        }
        if (request.getPosition() != null && request.getPosition() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image position must not be negative");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Image is too large");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported image type");
        }
    }

    private String safeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file name is required");
        }
        try {
            String normalizedName = originalName.replace('\\', '/');
            String fileName = Path.of(normalizedName).getFileName().toString();
            if (fileName.isBlank() || fileName.length() > 255) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image file name");
            }
            return fileName;
        } catch (InvalidPathException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image file name", exception);
        }
    }

    private String createStorageKey(Long userId, String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        String extension = dotIndex >= 0 ? fileName.substring(dotIndex).toLowerCase(Locale.ROOT) : "";
        if (extension.length() > 10 || !extension.matches("\\.[a-z0-9]+")) {
            extension = "";
        }
        return "user-images/" + userId + "/" + UUID.randomUUID() + extension;
    }
}
