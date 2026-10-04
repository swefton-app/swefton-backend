package com.swefton.backend.modules.machine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.modules.image.dto.ImagePojo;
import com.swefton.backend.modules.image.entity.Image;
import com.swefton.backend.modules.image.enums.ImageType;
import com.swefton.backend.modules.image.repository.ImageRepository;
import com.swefton.backend.modules.image.service.ImageService;
import com.swefton.backend.modules.machine.dto.MachineRequest;
import com.swefton.backend.modules.machine.dto.MachineResponse;
import com.swefton.backend.modules.machine.dto.MachineMovementRequest;
import com.swefton.backend.modules.machine.entity.Machine;
import com.swefton.backend.modules.machine.entity.MachineMovement;
import com.swefton.backend.modules.machine.enums.MachineMuscleGroup;
import com.swefton.backend.modules.machine.repository.MachineRepository;
import com.swefton.backend.modules.machine.repository.FacilityMachineRepository;
import com.swefton.backend.modules.machine.repository.MachineMovementRepository;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.video.entity.Video;
import com.swefton.backend.modules.video.repository.VideoRepository;
import com.swefton.backend.modules.video.service.VideoService;
import com.swefton.backend.session.ISessionUser;

@ExtendWith(MockitoExtension.class)
class MachineServiceTests {

    @Mock
    private MachineRepository repository;

    @Mock
    private FacilityMachineRepository facilityMachineRepository;

    @Mock
    private MachineMovementRepository movementRepository;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private VideoService videoService;

    @Mock
    private ISessionUser sessionUser;

    private MachineService service;

    @BeforeEach
    void setUp() {
        service = new MachineService(
                repository,
                facilityMachineRepository,
                movementRepository,
                imageRepository,
                imageService,
                videoRepository,
                videoService,
                sessionUser);
    }

    @Test
    void createConnectsMachineToExistingImageEntity() {
        Image image = image(9L);
        when(sessionUser.getUserId()).thenReturn(7L);
        when(imageRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(java.util.Set.of(9L), 7L))
                .thenReturn(java.util.List.of(image));
        when(videoRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(java.util.Set.of(30L, 31L), 7L))
                .thenReturn(java.util.List.of(video(30L), video(31L)));
        doAnswer(invocation -> {
            Machine machine = invocation.getArgument(0);
            machine.setId(5L);
            return machine;
        }).when(repository).saveAndFlush(any(Machine.class));

        MachineResponse response = service.create(request("PRESS-01", 9L));

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.images()).singleElement().extracting(ImagePojo::getId).isEqualTo(9L);
        assertThat(response.images().getFirst().getUrl()).isEqualTo("/api/v1/images/9");
        assertThat(response.movements()).hasSize(2);
        assertThat(response.movements().get(0).muscleGroup()).isEqualTo("Chest");
        assertThat(response.movements().get(1).muscleGroup()).isEqualTo("Back");

        ArgumentCaptor<Machine> captor = ArgumentCaptor.forClass(Machine.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(image.getUser()).isNull();
        assertThat(image.getMachine().getId()).isEqualTo(5L);
    }

    @Test
    void createAllowsMovementWithoutVideo() {
        Image image = image(9L);
        when(sessionUser.getUserId()).thenReturn(7L);
        when(imageRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(java.util.Set.of(9L), 7L))
                .thenReturn(java.util.List.of(image));
        doAnswer(invocation -> {
            Machine machine = invocation.getArgument(0);
            machine.setId(5L);
            return machine;
        }).when(repository).saveAndFlush(any(Machine.class));

        MachineRequest request = new MachineRequest(
                "Leg Press",
                "LEG-01",
                null,
                "GYM",
                java.util.List.of(new MachineMovementRequest(
                        null,
                        "Leg Press",
                        MachineMuscleGroup.LEGS,
                        null,
                        null,
                        0)),
                java.util.List.of(9L));

        MachineResponse response = service.create(request);

        assertThat(response.movements()).hasSize(1);
        assertThat(response.movements().getFirst().videoId()).isNull();
        assertThat(response.movements().getFirst().videoUrl()).isNull();
        verifyNoInteractions(videoRepository);
    }

    @Test
    void updateReplacesAndDeletesPreviousImage() {
        Image previousImage = image(9L);
        Image replacementImage = image(10L);
        Machine machine = machine(previousImage);
        when(repository.findById(5L)).thenReturn(Optional.of(machine));
        when(imageRepository.findAllByMachineIdAndDeletedAtIsNullOrderByPositionAscCreatedAtDesc(5L))
                .thenReturn(java.util.List.of(previousImage));
        when(sessionUser.getUserId()).thenReturn(7L);
        when(imageRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(java.util.Set.of(10L), 7L))
                .thenReturn(java.util.List.of(replacementImage));
        when(videoRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(java.util.Set.of(30L, 31L), 7L))
                .thenReturn(java.util.List.of(video(30L), video(31L)));
        when(repository.saveAndFlush(machine)).thenReturn(machine);

        MachineResponse response = service.update(5L, request("PRESS-02", 10L));

        assertThat(response.images()).singleElement().extracting(ImagePojo::getId).isEqualTo(10L);
        assertThat(replacementImage.getUser()).isNull();
        assertThat(replacementImage.getMachine()).isSameAs(machine);
        verify(imageService).deleteAssignedImage(previousImage);
    }

    @Test
    void deleteRemovesMachineAndItsImage() {
        Machine machine = machine(image(9L));
        when(repository.findById(5L)).thenReturn(Optional.of(machine));
        Image machineImage = machineImage(machine, 9L);
        when(imageRepository.findAllByMachineIdAndDeletedAtIsNullOrderByPositionAscCreatedAtDesc(5L))
                .thenReturn(java.util.List.of(machineImage));

        service.delete(5L);

        verify(repository).delete(machine);
        verify(repository).flush();
        verify(imageService).deleteAssignedImagePermanently(machineImage);
        verify(videoService).deleteAssignedVideoPermanently(machine.getMovements().getFirst().getVideo());
    }

    @Test
    void createRejectsMoreThanOneMovementForTheSameMuscleGroup() {
        MachineRequest request = new MachineRequest(
                "Chest Press",
                "PRESS-01",
                null,
                "GYM",
                java.util.List.of(
                        new MachineMovementRequest(
                                null,
                                "Flat Chest Press",
                                MachineMuscleGroup.CHEST,
                                30L,
                                "Press forward",
                                0),
                        new MachineMovementRequest(
                                null,
                                "Incline Chest Press",
                                MachineMuscleGroup.CHEST,
                                31L,
                                "Press upward",
                                1)),
                java.util.List.of(9L));

        when(sessionUser.getUserId()).thenReturn(7L);
        when(imageRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(java.util.Set.of(9L), 7L))
                .thenReturn(java.util.List.of(image(9L)));
        when(videoRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(java.util.Set.of(30L, 31L), 7L))
                .thenReturn(java.util.List.of(video(30L), video(31L)));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("only one movement per machine");
    }

    private MachineRequest request(String code, Long imageId) {
        return new MachineRequest(
                " Chest Press ",
                code,
                " Upper-body machine ",
                "GYM",
                java.util.List.of(new MachineMovementRequest(
                        null,
                        "Chest Press",
                        MachineMuscleGroup.CHEST,
                        30L,
                        "Keep your back against the pad",
                        0),
                        new MachineMovementRequest(
                                null,
                                "Supported Row",
                                MachineMuscleGroup.BACK,
                                31L,
                                "Pull the handles toward your body",
                                1)),
                java.util.List.of(imageId));
    }

    private Image image(Long id) {
        User user = new User();
        user.setId(7L);
        Image image = new Image();
        image.setId(id);
        image.assignToUser(user);
        image.setType(ImageType.MACHINE);
        image.setFilePath("user-images/7/" + id + ".png");
        image.setOriginalName("machine.png");
        image.setContentType("image/png");
        image.setFileSize(5L);
        return image;
    }

    private Machine machine(Image image) {
        Machine machine = new Machine();
        machine.setId(5L);
        machine.setName("Chest Press");
        machine.setCode("PRESS-01");
        machine.setFacilityCategory("GYM");
        MachineMovement movement = new MachineMovement();
        movement.setId(20L);
        movement.setMachine(machine);
        movement.setName("Chest Press");
        movement.setMuscleGroup(MachineMuscleGroup.CHEST);
        movement.assignVideo(video(30L));
        movement.setPosition(0);
        machine.getMovements().add(movement);
        image.assignToMachine(machine);
        return machine;
    }

    private Image machineImage(Machine machine, Long id) {
        Image image = image(id);
        image.assignToMachine(machine);
        return image;
    }

    private Video video(Long id) {
        User user = new User();
        user.setId(7L);
        Video video = new Video();
        video.setId(id);
        video.setUser(user);
        video.setFilePath("user-videos/7/" + id + ".mp4");
        video.setOriginalName("movement-" + id + ".mp4");
        video.setContentType("video/mp4");
        video.setFileSize(5L);
        return video;
    }
}
