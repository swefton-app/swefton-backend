package com.swefton.backend.modules.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.infrastructure.storage.ObjectStorage;
import com.swefton.backend.infrastructure.web.response.FileResponseHelper;
import com.swefton.backend.modules.document.dto.DocumentResponse;
import com.swefton.backend.modules.document.entity.Document;
import com.swefton.backend.modules.document.entity.UserDocument;
import com.swefton.backend.modules.document.enums.DocumentType;
import com.swefton.backend.modules.document.repository.DocumentRepository;
import com.swefton.backend.modules.document.repository.UserDocumentRepository;
import com.swefton.backend.modules.user.entity.Role;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.enums.RoleCode;
import com.swefton.backend.modules.user.repository.UserRepository;
import com.swefton.backend.session.ISessionUser;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTests {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserDocumentRepository userDocumentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectStorage objectStorage;

    private final FileResponseHelper fileResponseHelper = new FileResponseHelper();

    @Mock
    private ISessionUser sessionUser;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(
                documentRepository,
                userDocumentRepository,
                userRepository,
                objectStorage,
                fileResponseHelper,
                sessionUser);
        ReflectionTestUtils.setField(documentService, "maxFileSizeBytes", 20L * 1024 * 1024);
    }

    @Test
    void uploadStoresFileDocumentAndUserDocument() {
        User trainer = trainer();
        MockMultipartFile file = new MockMultipartFile(
                "file", "certificate.pdf", "application/pdf", "test".getBytes());

        when(sessionUser.getUserId()).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(trainer));
        when(objectStorage.store(anyString(), any(InputStream.class))).thenReturn(4L);
        doAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            document.setId(11L);
            document.prePersist();
            return document;
        }).when(documentRepository).saveAndFlush(any(Document.class));

        DocumentResponse response = documentService.upload(file, DocumentType.LICENCE);

        assertThat(response.id()).isEqualTo(11L);
        assertThat(response.fileName()).isEqualTo("certificate.pdf");
        assertThat(response.contentType()).isEqualTo("application/pdf");
        assertThat(response.type()).isEqualTo(DocumentType.LICENCE);
        assertThat(response.size()).isEqualTo(4L);

        ArgumentCaptor<UserDocument> linkCaptor = ArgumentCaptor.forClass(UserDocument.class);
        verify(userDocumentRepository).saveAndFlush(linkCaptor.capture());
        assertThat(linkCaptor.getValue().getUser()).isSameAs(trainer);
        assertThat(linkCaptor.getValue().getDocument().getId()).isEqualTo(11L);
        assertThat(linkCaptor.getValue().getType()).isEqualTo(DocumentType.LICENCE);
    }

    @Test
    void uploadRejectsUnsupportedContentTypeBeforeWriting() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.exe", "application/octet-stream", "test".getBytes());

        assertThatThrownBy(() -> documentService.upload(file, DocumentType.OTHER))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("415 UNSUPPORTED_MEDIA_TYPE");

        verify(objectStorage, never()).store(anyString(), any(InputStream.class));
        verify(documentRepository, never()).saveAndFlush(any(Document.class));
    }

    @Test
    void uploadRequiresDocumentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "certificate.pdf", "application/pdf", "test".getBytes());

        assertThatThrownBy(() -> documentService.upload(file, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");

        verify(objectStorage, never()).store(anyString(), any(InputStream.class));
    }

    @Test
    void uploadDeletesStoredFileWhenDatabaseWriteFails() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "certificate.pdf", "application/pdf", "test".getBytes());

        when(sessionUser.getUserId()).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(trainer()));
        when(objectStorage.store(anyString(), any(InputStream.class))).thenReturn(4L);
        doThrow(new IllegalStateException("database unavailable"))
                .when(documentRepository).saveAndFlush(any(Document.class));

        assertThatThrownBy(() -> documentService.upload(file, DocumentType.LICENCE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(objectStorage).delete(keyCaptor.capture());
        assertThat(keyCaptor.getValue()).startsWith("trainer-documents/7/").endsWith(".pdf");
    }

    @Test
    void downloadBuildsCompleteFileResponseInService() {
        User trainer = trainer();
        Document document = new Document();
        document.setId(11L);
        document.setOriginalFileName("certificate.pdf");
        document.setStorageKey("trainer-documents/7/document.pdf");
        document.setContentType(MediaType.APPLICATION_PDF_VALUE);
        document.setFileSize(4L);
        UserDocument userDocument = new UserDocument();
        userDocument.setUser(trainer);
        userDocument.setDocument(document);
        ByteArrayResource resource = new ByteArrayResource("test".getBytes());

        when(sessionUser.getUserId()).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(trainer));
        when(userDocumentRepository.findByUserIdAndDocumentIdAndDeletedAtIsNull(7L, 11L))
                .thenReturn(Optional.of(userDocument));
        when(objectStorage.load(document.getStorageKey())).thenReturn(resource);

        ResponseEntity<org.springframework.core.io.Resource> response = documentService.download(11L);

        assertThat(response.getBody()).isSameAs(resource);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getContentLength()).isEqualTo(4L);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment")
                .contains("certificate.pdf");
    }

    private User trainer() {
        Role role = new Role();
        role.setCode(RoleCode.TRAINER);
        User trainer = new User();
        trainer.setId(7L);
        trainer.setRole(role);
        return trainer;
    }
}
