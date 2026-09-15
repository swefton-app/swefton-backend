package com.swefton.backend.modules.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

import com.swefton.backend.infrastructure.storage.ObjectStorage;
import com.swefton.backend.modules.document.dto.DocumentResponse;
import com.swefton.backend.modules.document.dto.request.GenerateCvRequest;
import com.swefton.backend.modules.document.entity.Document;
import com.swefton.backend.modules.document.entity.UserDocument;
import com.swefton.backend.modules.document.enums.CvTemplate;
import com.swefton.backend.modules.document.enums.DocumentType;
import com.swefton.backend.modules.document.repository.DocumentRepository;
import com.swefton.backend.modules.document.repository.UserDocumentRepository;
import com.swefton.backend.modules.user.entity.Role;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.enums.RoleCode;
import com.swefton.backend.modules.user.repository.UserRepository;
import com.swefton.backend.session.ISessionUser;

@ExtendWith(MockitoExtension.class)
class CvDocumentServiceTests {

    @Mock private UserRepository userRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private UserDocumentRepository userDocumentRepository;
    @Mock private CvPdfService cvPdfService;
    @Mock private ObjectStorage objectStorage;
    @Mock private ISessionUser sessionUser;

    private CvDocumentService service;

    @BeforeEach
    void setUp() {
        service = new CvDocumentService(
                userRepository,
                documentRepository,
                userDocumentRepository,
                cvPdfService,
                objectStorage,
                sessionUser);
    }

    @Test
    void createsTheFirstCvAndLinksItToTrainer() {
        User trainer = trainer();
        GenerateCvRequest request = request();
        byte[] pdf = "%PDF-test".getBytes();
        when(sessionUser.getUserId()).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(trainer));
        when(cvPdfService.generate(request)).thenReturn(pdf);
        when(userDocumentRepository.findByUserIdAndTypeAndDeletedAtIsNull(7L, DocumentType.CV))
                .thenReturn(Optional.empty());
        when(documentRepository.saveAndFlush(any(Document.class))).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            document.setId(11L);
            document.prePersist();
            return document;
        });

        DocumentResponse response = service.generate(request);

        assertThat(response.id()).isEqualTo(11L);
        assertThat(response.fileName()).isEqualTo("franko-kaloshi-cv.pdf");
        assertThat(response.type()).isEqualTo(DocumentType.CV);
        ArgumentCaptor<UserDocument> link = ArgumentCaptor.forClass(UserDocument.class);
        verify(userDocumentRepository).saveAndFlush(link.capture());
        assertThat(link.getValue().getUser()).isSameAs(trainer);
        assertThat(link.getValue().getType()).isEqualTo(DocumentType.CV);
        verify(objectStorage, never()).delete(any());
    }

    @Test
    void regeneratingUpdatesTheExistingDocumentAndRemovesTheOldFile() {
        User trainer = trainer();
        GenerateCvRequest request = request();
        Document document = new Document();
        document.setId(11L);
        document.setStorageKey("trainer-documents/7/cv/old.pdf");
        UserDocument link = new UserDocument();
        link.setId(12L);
        link.setUser(trainer);
        link.setDocument(document);
        link.setType(DocumentType.CV);

        when(sessionUser.getUserId()).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(trainer));
        when(cvPdfService.generate(request)).thenReturn("%PDF-new".getBytes());
        when(userDocumentRepository.findByUserIdAndTypeAndDeletedAtIsNull(7L, DocumentType.CV))
                .thenReturn(Optional.of(link));
        when(documentRepository.saveAndFlush(document)).thenReturn(document);

        DocumentResponse response = service.generate(request);

        assertThat(response.id()).isEqualTo(11L);
        assertThat(document.getStorageKey()).isNotEqualTo("trainer-documents/7/cv/old.pdf");
        verify(objectStorage).delete("trainer-documents/7/cv/old.pdf");
        verify(userDocumentRepository).saveAndFlush(link);
    }

    private User trainer() {
        Role role = new Role();
        role.setCode(RoleCode.TRAINER);
        User trainer = new User();
        trainer.setId(7L);
        trainer.setRole(role);
        return trainer;
    }

    private GenerateCvRequest request() {
        GenerateCvRequest request = new GenerateCvRequest();
        request.setTemplate(CvTemplate.MODERN);
        request.setFirstName("Franko");
        request.setLastName("Kaloshi");
        request.setProfessionalTitle("Personal Trainer");
        request.setEmail("franko@swefton.com");
        return request;
    }
}
