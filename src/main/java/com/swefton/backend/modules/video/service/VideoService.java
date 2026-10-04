package com.swefton.backend.modules.video.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.LocalDateTime;
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
import com.swefton.backend.modules.machine.repository.MachineMovementRepository;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.repository.UserRepository;
import com.swefton.backend.modules.video.dto.VideoResponse;
import com.swefton.backend.modules.video.entity.Video;
import com.swefton.backend.modules.video.repository.VideoRepository;
import com.swefton.backend.session.ISessionUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VideoService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "video/mp4",
            "video/webm",
            "video/quicktime",
            "video/x-m4v");

    private final VideoRepository videoRepository;
    private final MachineMovementRepository movementRepository;
    private final UserRepository userRepository;
    private final ObjectStorage objectStorage;
    private final FileResponseHelper fileResponseHelper;
    private final ISessionUser sessionUser;

    @Value("${app.videos.max-file-size-bytes:209715200}")
    private long maxFileSizeBytes;

    @Transactional
    public VideoResponse upload(MultipartFile file) {
        validateUpload(file);
        User user = currentUser();
        String originalName = safeOriginalName(file.getOriginalFilename());
        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        String storageKey = createStorageKey(user.getId(), originalName);

        try (InputStream content = file.getInputStream()) {
            long storedSize = objectStorage.store(storageKey, content);
            try {
                Video video = new Video();
                video.setUser(user);
                video.setFilePath(storageKey);
                video.setOriginalName(originalName);
                video.setContentType(contentType);
                video.setFileSize(storedSize);
                return new VideoResponse(videoRepository.saveAndFlush(video));
            } catch (RuntimeException exception) {
                objectStorage.delete(storageKey);
                throw exception;
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not read uploaded video",
                    exception);
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> getContent(Long id) {
        Video video = videoRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));
        return fileResponseHelper.inline(
                objectStorage.load(video.getFilePath()),
                video.getOriginalName(),
                video.getContentType(),
                video.getFileSize());
    }

    @Transactional
    public void delete(Long id) {
        Video video = ownedVideo(id);
        if (movementRepository.existsByVideoId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Video is assigned to a machine movement");
        }
        video.setDeletedAt(LocalDateTime.now());
        videoRepository.saveAndFlush(video);
        objectStorage.delete(video.getFilePath());
    }

    @Transactional
    public void deleteAssignedVideoPermanently(Video video) {
        if (video == null) {
            return;
        }
        objectStorage.delete(video.getFilePath());
        videoRepository.delete(video);
        videoRepository.flush();
    }

    private User currentUser() {
        return userRepository.findById(sessionUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Video ownedVideo(Long id) {
        return videoRepository.findByIdAndUserIdAndDeletedAtIsNull(id, sessionUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Video must not be empty");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Video is too large");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported video type");
        }
    }

    private String safeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Video file name is required");
        }
        try {
            String fileName = Path.of(originalName.replace('\\', '/')).getFileName().toString();
            if (fileName.isBlank() || fileName.length() > 255) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid video file name");
            }
            return fileName;
        } catch (InvalidPathException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid video file name", exception);
        }
    }

    private String createStorageKey(Long userId, String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        String extension = dotIndex >= 0 ? fileName.substring(dotIndex).toLowerCase(Locale.ROOT) : "";
        if (extension.length() > 10 || !extension.matches("\\.[a-z0-9]+")) {
            extension = "";
        }
        return "user-videos/" + userId + "/" + UUID.randomUUID() + extension;
    }
}
