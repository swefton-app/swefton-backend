package com.swefton.backend.modules.document.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
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
import com.swefton.backend.modules.document.dto.DocumentResponse;
import com.swefton.backend.modules.document.entity.Document;
import com.swefton.backend.modules.document.entity.UserDocument;
import com.swefton.backend.modules.document.enums.DocumentType;
import com.swefton.backend.modules.document.repository.DocumentRepository;
import com.swefton.backend.modules.document.repository.UserDocumentRepository;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.enums.RoleCode;
import com.swefton.backend.modules.user.repository.UserRepository;
import com.swefton.backend.session.ISessionUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg",
            "image/png");

    private final DocumentRepository documentRepository;
    private final UserDocumentRepository userDocumentRepository;
    private final UserRepository userRepository;
    private final ObjectStorage objectStorage;
    private final FileResponseHelper fileResponseHelper;
    private final ISessionUser sessionUser;

    @Value("${app.documents.max-file-size-bytes:20971520}")
    private long maxFileSizeBytes;

    @Transactional
    public DocumentResponse upload(MultipartFile file, DocumentType type) {
        validate(file);
        if (type == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document type is required");
        }
        User trainer = currentTrainer();
        String originalFileName = safeOriginalFileName(file.getOriginalFilename());
        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        String storageKey = createStorageKey(trainer.getId(), originalFileName);

        try (InputStream content = file.getInputStream()) {
            long storedSize = objectStorage.store(storageKey, content);
            try {
                Document document = new Document();
                document.setOriginalFileName(originalFileName);
                document.setStorageKey(storageKey);
                document.setContentType(contentType);
                document.setType(type);
                document.setFileSize(storedSize);
                document = documentRepository.saveAndFlush(document);

                UserDocument userDocument = new UserDocument();
                userDocument.setUser(trainer);
                userDocument.setDocument(document);
                userDocument.setType(type);
                userDocumentRepository.saveAndFlush(userDocument);
                return toResponse(document);
            } catch (RuntimeException exception) {
                objectStorage.delete(storageKey);
                throw exception;
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not read uploaded document",
                    exception);
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getCurrentTrainerDocuments() {
        Long trainerId = currentTrainer().getId();
        return userDocumentRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(trainerId)
                .stream()
                .map(UserDocument::getDocument)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> download(Long documentId) {
        Document document = ownedDocument(documentId);
        Resource resource = objectStorage.load(document.getStorageKey());
        return fileResponseHelper.download(
                resource,
                document.getOriginalFileName(),
                document.getContentType(),
                document.getFileSize());
    }

    @Transactional
    public void delete(Long documentId) {
        UserDocument userDocument = ownedUserDocument(documentId);
        Document document = userDocument.getDocument();
        userDocumentRepository.delete(userDocument);
        userDocumentRepository.flush();
        documentRepository.delete(document);
        documentRepository.flush();
        objectStorage.delete(document.getStorageKey());
    }

    private User currentTrainer() {
        User user = userRepository.findById(sessionUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() == null || user.getRole().getCode() != RoleCode.TRAINER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only trainers can manage documents");
        }
        return user;
    }

    private Document ownedDocument(Long documentId) {
        return ownedUserDocument(documentId).getDocument();
    }

    private UserDocument ownedUserDocument(Long documentId) {
        return userDocumentRepository.findByUserIdAndDocumentIdAndDeletedAtIsNull(currentTrainer().getId(), documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document must not be empty");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Document is too large");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported document type");
        }
    }

    private String safeOriginalFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document file name is required");
        }
        try {
            String normalizedName = originalFileName.replace('\\', '/');
            String fileName = Path.of(normalizedName).getFileName().toString();
            if (fileName.isBlank() || fileName.length() > 255) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid document file name");
            }
            return fileName;
        } catch (InvalidPathException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid document file name", exception);
        }
    }

    private String createStorageKey(Long trainerId, String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        String extension = dotIndex >= 0 ? fileName.substring(dotIndex).toLowerCase(Locale.ROOT) : "";
        if (extension.length() > 15 || !extension.matches("\\.[a-z0-9]+")) {
            extension = "";
        }
        return "trainer-documents/" + trainerId + "/" + UUID.randomUUID() + extension;
    }

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getOriginalFileName(),
                document.getContentType(),
                document.getType(),
                document.getFileSize(),
                document.getCreatedAt());
    }
}
