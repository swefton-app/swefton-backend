package com.swefton.backend.modules.video.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
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

@ExtendWith(MockitoExtension.class)
class VideoServiceTests {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private MachineMovementRepository movementRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectStorage objectStorage;

    @Mock
    private ISessionUser sessionUser;

    private VideoService videoService;

    @BeforeEach
    void setUp() {
        videoService = new VideoService(
                videoRepository,
                movementRepository,
                userRepository,
                objectStorage,
                new FileResponseHelper(),
                sessionUser);
        ReflectionTestUtils.setField(videoService, "maxFileSizeBytes", 200L * 1024 * 1024);
    }

    @Test
    void uploadStoresVideoAndReturnsPlaybackUrl() {
        User user = new User();
        user.setId(42L);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "chest-press.mp4",
                "video/mp4",
                "video".getBytes());

        when(sessionUser.getUserId()).thenReturn(42L);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(objectStorage.store(anyString(), any(InputStream.class))).thenReturn(5L);
        doAnswer(invocation -> {
            Video video = invocation.getArgument(0);
            video.setId(9L);
            video.prePersist();
            return video;
        }).when(videoRepository).saveAndFlush(any(Video.class));

        VideoResponse response = videoService.upload(file);

        assertThat(response.id()).isEqualTo(9L);
        assertThat(response.url()).isEqualTo("/api/v1/videos/9");
        assertThat(response.originalName()).isEqualTo("chest-press.mp4");
        ArgumentCaptor<Video> captor = ArgumentCaptor.forClass(Video.class);
        verify(videoRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getFilePath()).startsWith("user-videos/42/");
    }

    @Test
    void uploadRejectsUnsupportedContentTypeBeforeWriting() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "movement.avi",
                "video/x-msvideo",
                "video".getBytes());

        assertThatThrownBy(() -> videoService.upload(file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("415 UNSUPPORTED_MEDIA_TYPE");

        verify(objectStorage, never()).store(anyString(), any(InputStream.class));
    }

    @Test
    void deleteRejectsVideoThatIsAssignedToMovement() {
        User user = new User();
        user.setId(42L);
        Video video = new Video();
        video.setId(9L);
        video.setUser(user);
        video.setFilePath("user-videos/42/video.mp4");

        when(sessionUser.getUserId()).thenReturn(42L);
        when(videoRepository.findByIdAndUserIdAndDeletedAtIsNull(9L, 42L))
                .thenReturn(Optional.of(video));
        when(movementRepository.existsByVideoId(9L)).thenReturn(true);

        assertThatThrownBy(() -> videoService.delete(9L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        verify(videoRepository, never()).saveAndFlush(any(Video.class));
        verify(objectStorage, never()).delete(anyString());
    }
}
