package com.swefton.backend.modules.document.service;

import java.io.ByteArrayInputStream;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.infrastructure.storage.ObjectStorage;
import com.swefton.backend.modules.document.dto.DocumentResponse;
import com.swefton.backend.modules.document.dto.request.GenerateCvRequest;
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
public class CvDocumentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CvDocumentService.class);
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final UserDocumentRepository userDocumentRepository;
    private final CvPdfService cvPdfService;
    private final ObjectStorage objectStorage;
    private final ISessionUser sessionUser;

    @Transactional
    public DocumentResponse generate(GenerateCvRequest request) {
        User trainer = currentTrainer();
        byte[] pdf = cvPdfService.generate(request);
        String fileName = createFileName(request.getFirstName(), request.getLastName());
        String storageKey = createStorageKey(trainer.getId());
        objectStorage.store(storageKey, new ByteArrayInputStream(pdf));

        UserDocument userDocument = userDocumentRepository
                .findByUserIdAndTypeAndDeletedAtIsNull(trainer.getId(), DocumentType.CV)
                .orElse(null);
        String previousStorageKey = userDocument == null
                ? null
                : userDocument.getDocument().getStorageKey();
        boolean lifecycleManaged = registerStorageLifecycle(storageKey, previousStorageKey);

        try {
            Document document;
            if (userDocument == null) {
                document = new Document();
                userDocument = new UserDocument();
                userDocument.setUser(trainer);
                userDocument.setDocument(document);
                userDocument.setType(DocumentType.CV);
            } else {
                document = userDocument.getDocument();
                userDocument.setType(DocumentType.CV);
            }

            document.setOriginalFileName(fileName);
            document.setStorageKey(storageKey);
            document.setContentType(PDF_CONTENT_TYPE);
            document.setType(DocumentType.CV);
            document.setFileSize(pdf.length);
            document = documentRepository.saveAndFlush(document);

            if (userDocument.getId() == null) {
                userDocument.setDocument(document);
                userDocumentRepository.saveAndFlush(userDocument);
            } else {
                userDocumentRepository.saveAndFlush(userDocument);
            }

            if (!lifecycleManaged && previousStorageKey != null
                    && !previousStorageKey.equals(storageKey)) {
                deleteQuietly(previousStorageKey);
            }

            return toResponse(document);
        } catch (RuntimeException exception) {
            if (!lifecycleManaged) {
                deleteQuietly(storageKey);
            }
            throw exception;
        }
    }

    private User currentTrainer() {
        User user = userRepository.findById(sessionUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() == null || user.getRole().getCode() != RoleCode.TRAINER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only trainers can generate a professional CV");
        }
        return user;
    }

    private String createFileName(String firstName, String lastName) {
        String name = slug(firstName + "-" + lastName);
        return (name.isBlank() ? "trainer" : name) + "-cv.pdf";
    }

    private String createStorageKey(Long trainerId) {
        return "trainer-documents/" + trainerId + "/cv/" + UUID.randomUUID() + ".pdf";
    }

    private String slug(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private boolean registerStorageLifecycle(String newKey, String previousKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    if (previousKey != null && !previousKey.equals(newKey)) {
                        deleteQuietly(previousKey);
                    }
                } else {
                    deleteQuietly(newKey);
                }
            }
        });
        return true;
    }

    private void deleteQuietly(String storageKey) {
        try {
            objectStorage.delete(storageKey);
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not remove superseded CV file {}", storageKey, exception);
        }
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
