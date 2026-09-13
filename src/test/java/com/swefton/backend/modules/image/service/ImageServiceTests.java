package com.swefton.backend.modules.image.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
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
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.swefton.backend.infrastructure.storage.ObjectStorage;
import com.swefton.backend.infrastructure.web.response.FileResponseHelper;
import com.swefton.backend.modules.image.dto.ImagePojo;
import com.swefton.backend.modules.image.entity.Image;
import com.swefton.backend.modules.image.enums.ImageType;
import com.swefton.backend.modules.image.repository.ImageRepository;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.repository.UserRepository;
import com.swefton.backend.session.ISessionUser;

@ExtendWith(MockitoExtension.class)
class ImageServiceTests {

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectStorage objectStorage;

    @Mock
    private ISessionUser sessionUser;

    private ImageService imageService;

    @BeforeEach
    void setUp() {
        imageService = new ImageService(
                imageRepository,
                userRepository,
                objectStorage,
                new FileResponseHelper(),
                sessionUser);
        ReflectionTestUtils.setField(imageService, "maxFileSizeBytes", 10L * 1024 * 1024);
    }

    @Test
    void uploadStoresImageAndReturnsImagePojo() {
        User user = new User();
        user.setId(42L);
        ImagePojo request = new ImagePojo();
        request.setType(ImageType.PROFILE);
        request.setPosition(0);
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", MediaType.IMAGE_PNG_VALUE, "image".getBytes());

        when(sessionUser.getUserId()).thenReturn(42L);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(objectStorage.store(anyString(), any(InputStream.class))).thenReturn(5L);
        doAnswer(invocation -> {
            Image image = invocation.getArgument(0);
            image.setId(9L);
            image.prePersist();
            return image;
        }).when(imageRepository).saveAndFlush(any(Image.class));

        ImagePojo response = imageService.upload(file, request);

        assertThat(response.getId()).isEqualTo(9L);
        assertThat(response.getUserId()).isEqualTo(42L);
        assertThat(response.getType()).isEqualTo(ImageType.PROFILE);
        assertThat(response.getOriginalName()).isEqualTo("avatar.png");
        assertThat(response.getUrl()).isEqualTo("/api/v1/images/9");

        ArgumentCaptor<Image> imageCaptor = ArgumentCaptor.forClass(Image.class);
        verify(imageRepository).saveAndFlush(imageCaptor.capture());
        assertThat(imageCaptor.getValue().getFilePath()).startsWith("user-images/42/");
    }

    @Test
    void getContentReturnsActualImageResourceInline() {
        Image image = new Image();
        image.setId(9L);
        image.setOriginalName("avatar.png");
        image.setContentType(MediaType.IMAGE_PNG_VALUE);
        image.setFileSize(5L);
        image.setFilePath("user-images/42/avatar.png");
        ByteArrayResource resource = new ByteArrayResource("image".getBytes());

        when(imageRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(image));
        when(objectStorage.load(image.getFilePath())).thenReturn(resource);

        ResponseEntity<Resource> response = imageService.getContent(9L);

        assertThat(response.getBody()).isSameAs(resource);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("inline")
                .contains("avatar.png");
    }
}
