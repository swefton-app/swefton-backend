package com.swefton.backend.modules.machine.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.modules.facility.enums.FacilityCategory;
import com.swefton.backend.modules.image.dto.ImagePojo;
import com.swefton.backend.modules.image.entity.Image;
import com.swefton.backend.modules.image.enums.ImageType;
import com.swefton.backend.modules.image.repository.ImageRepository;
import com.swefton.backend.modules.image.service.ImageService;
import com.swefton.backend.modules.machine.dto.MachineRequest;
import com.swefton.backend.modules.machine.dto.MachineResponse;
import com.swefton.backend.modules.machine.dto.MachineMovementRequest;
import com.swefton.backend.modules.machine.dto.MachineMovementResponse;
import com.swefton.backend.modules.machine.entity.Machine;
import com.swefton.backend.modules.machine.entity.MachineMovement;
import com.swefton.backend.modules.machine.enums.MachineMuscleGroup;
import com.swefton.backend.modules.machine.repository.MachineRepository;
import com.swefton.backend.modules.machine.repository.FacilityMachineRepository;
import com.swefton.backend.modules.machine.repository.MachineMovementRepository;
import com.swefton.backend.session.ISessionUser;
import com.swefton.backend.modules.video.entity.Video;
import com.swefton.backend.modules.video.repository.VideoRepository;
import com.swefton.backend.modules.video.service.VideoService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MachineService {

    private final MachineRepository repository;
    private final FacilityMachineRepository facilityMachineRepository;
    private final MachineMovementRepository movementRepository;
    private final ImageRepository imageRepository;
    private final ImageService imageService;
    private final VideoRepository videoRepository;
    private final VideoService videoService;
    private final ISessionUser sessionUser;

    @Transactional(readOnly = true)
    public List<MachineResponse> findAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MachineResponse findById(Long id) {
        return toResponse(findEntityById(id));
    }

    @Transactional
    public MachineResponse create(MachineRequest request) {
        requireUniqueCode(request.code(), null);
        Machine machine = new Machine();
        List<Image> images = resolveImages(null, request.imageIds());
        Map<Long, Video> videos = resolveVideos(null, request.movements());
        applyRequest(machine, request, videos);
        Machine saved = repository.saveAndFlush(machine);
        images.forEach(image -> image.assignToMachine(saved));
        imageRepository.saveAllAndFlush(images);
        return toResponse(saved, images);
    }

    @Transactional
    public MachineResponse update(Long id, MachineRequest request) {
        Machine machine = findEntityById(id);
        requireUniqueCode(request.code(), id);
        List<Image> previousImages = activeImages(machine.getId());
        List<Image> replacementImages = resolveImages(machine.getId(), request.imageIds());
        List<Video> previousVideos = machine.getMovements().stream()
                .map(MachineMovement::getVideo)
                .filter(java.util.Objects::nonNull)
                .toList();
        Map<Long, Video> replacementVideos = resolveVideos(machine.getId(), request.movements());

        applyRequest(machine, request, replacementVideos);
        Machine saved = repository.saveAndFlush(machine);
        replacementImages.forEach(image -> image.assignToMachine(saved));
        imageRepository.saveAllAndFlush(replacementImages);

        Set<Long> replacementIds = replacementImages.stream().map(Image::getId).collect(java.util.stream.Collectors.toSet());
        previousImages.stream()
                .filter(image -> !replacementIds.contains(image.getId()))
                .forEach(imageService::deleteAssignedImage);
        Set<Long> replacementVideoIds = new HashSet<>(replacementVideos.keySet());
        previousVideos.stream()
                .filter(video -> !replacementVideoIds.contains(video.getId()))
                .forEach(videoService::deleteAssignedVideoPermanently);
        return toResponse(saved, replacementImages);
    }

    @Transactional
    public void delete(Long id) {
        Machine machine = findEntityById(id);
        if (facilityMachineRepository.existsByMachineId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Machine cannot be deleted while it is registered in a facility");
        }
        List<Video> videos = machine.getMovements().stream()
                .map(MachineMovement::getVideo)
                .filter(java.util.Objects::nonNull)
                .toList();
        activeImages(machine.getId()).forEach(imageService::deleteAssignedImagePermanently);
        repository.delete(machine);
        repository.flush();
        videos.forEach(videoService::deleteAssignedVideoPermanently);
    }

    private List<Image> resolveImages(Long machineId, List<Long> requestedIds) {
        if (requestedIds == null || requestedIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one machine image is required");
        }
        if (requestedIds.stream().anyMatch(id -> id == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Machine image IDs cannot be null");
        }

        Set<Long> uniqueIds = new HashSet<>(requestedIds);
        if (uniqueIds.size() != requestedIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Machine image IDs must be unique");
        }

        Map<Long, Image> availableById = new HashMap<>();
        imageRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(
                        uniqueIds,
                        sessionUser.getUserId())
                .forEach(image -> availableById.put(image.getId(), image));
        if (machineId != null) {
            activeImages(machineId).stream()
                    .filter(image -> uniqueIds.contains(image.getId()))
                    .forEach(image -> availableById.put(image.getId(), image));
        }

        List<Image> images = new ArrayList<>();
        for (Long imageId : requestedIds) {
            Image image = availableById.get(imageId);
            if (image == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Machine image does not exist, is not owned by you, or belongs to another machine: "
                                + imageId);
            }
            if (!ImageType.MACHINE.equals(image.getType())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Machine image must have type " + ImageType.MACHINE);
            }
            images.add(image);
        }
        return images;
    }

    private List<Image> activeImages(Long machineId) {
        return imageRepository
                .findAllByMachineIdAndDeletedAtIsNullOrderByPositionAscCreatedAtDesc(machineId);
    }

    private Map<Long, Video> resolveVideos(
            Long machineId,
            List<MachineMovementRequest> requestedMovements) {
        if (requestedMovements == null || requestedMovements.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one machine movement is required");
        }

        Set<Long> requestedIds = new HashSet<>();
        for (MachineMovementRequest movement : requestedMovements) {
            if (movement.videoId() == null) {
                continue;
            }
            if (!requestedIds.add(movement.videoId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each movement must use a different video");
            }
        }

        Map<Long, Video> availableById = new HashMap<>();
        if (!requestedIds.isEmpty()) {
            videoRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(requestedIds, sessionUser.getUserId())
                    .forEach(video -> availableById.put(video.getId(), video));
        }
        if (machineId != null) {
            repository.findById(machineId).stream()
                    .flatMap(machine -> machine.getMovements().stream())
                    .map(MachineMovement::getVideo)
                    .filter(java.util.Objects::nonNull)
                    .filter(video -> requestedIds.contains(video.getId()))
                    .forEach(video -> availableById.put(video.getId(), video));
        }

        for (Long videoId : requestedIds) {
            if (!availableById.containsKey(videoId)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Movement video does not exist or is not owned by you: " + videoId);
            }
            boolean usedElsewhere = machineId == null
                    ? movementRepository.existsByVideoId(videoId)
                    : movementRepository.existsByVideoIdAndMachineIdNot(videoId, machineId);
            if (usedElsewhere) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Movement video is already assigned to another machine: " + videoId);
            }
        }
        return availableById;
    }

    private void applyRequest(
            Machine machine,
            MachineRequest request,
            Map<Long, Video> videos) {
        machine.setName(request.name().trim());
        machine.setCode(request.code().trim());
        machine.setDescription(trimToNull(request.description()));
        machine.setFacilityCategory(validatedFacilityCategory(request.facilityCategory()));
        synchronizeMovements(machine, request.movements(), videos);
    }

    private void synchronizeMovements(
            Machine machine,
            List<MachineMovementRequest> requestedMovements,
            Map<Long, Video> videos) {
        if (requestedMovements == null || requestedMovements.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one machine movement is required");
        }

        Map<Long, MachineMovement> existingById = new HashMap<>();
        machine.getMovements().stream()
                .filter(movement -> movement.getId() != null)
                .forEach(movement -> existingById.put(movement.getId(), movement));
        Set<Long> retainedIds = new HashSet<>();
        Set<Integer> usedPositions = new HashSet<>();
        Set<MachineMuscleGroup> usedMuscleGroups = new HashSet<>();

        for (int index = 0; index < requestedMovements.size(); index++) {
            MachineMovementRequest request = requestedMovements.get(index);
            MachineMovement movement;
            if (request.id() == null) {
                movement = new MachineMovement();
                movement.setMachine(machine);
                machine.getMovements().add(movement);
            } else {
                movement = existingById.get(request.id());
                if (machine.getId() == null || movement == null || !retainedIds.add(request.id())) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Machine movement does not belong to this machine: " + request.id());
                }
            }

            int position = request.position() == null ? index : request.position();
            if (!usedPositions.add(position)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Machine movement positions must be unique");
            }
            if (!usedMuscleGroups.add(request.muscleGroup())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Each muscle group can have only one movement per machine");
            }
            movement.setName(request.name().trim());
            movement.setMuscleGroup(request.muscleGroup());
            if (request.videoId() == null) {
                movement.clearVideo();
            } else {
                movement.assignVideo(videos.get(request.videoId()));
            }
            movement.setInstructions(trimToNull(request.instructions()));
            movement.setPosition(position);
        }

        machine.getMovements().removeIf(
                movement -> movement.getId() != null && !retainedIds.contains(movement.getId()));

    }

    private void requireUniqueCode(String code, Long currentId) {
        String normalizedCode = code.trim();
        boolean exists = currentId == null
                ? repository.existsByCode(normalizedCode)
                : repository.existsByCodeAndIdNot(normalizedCode, currentId);
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Machine code already exists");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String validatedFacilityCategory(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!FacilityCategory.exists(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Facility category is invalid");
        }
        return normalized;
    }

    private String humanize(String value) {
        String[] words = value.toLowerCase(Locale.ROOT).split("_");
        StringBuilder label = new StringBuilder();
        for (String word : words) {
            if (!label.isEmpty()) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return label.toString();
    }

    private Machine findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Machine not found"));
    }

    private MachineResponse toResponse(Machine machine) {
        return toResponse(machine, activeImages(machine.getId()));
    }

    private MachineResponse toResponse(Machine machine, List<Image> images) {
        return new MachineResponse(
                machine.getId(),
                machine.getName(),
                machine.getCode(),
                machine.getDescription(),
                humanize(machine.getFacilityCategory()),
                machine.getMovements().stream()
                        .sorted(Comparator.comparing(MachineMovement::getPosition))
                        .map(MachineMovementResponse::new)
                        .toList(),
                images.stream().map(ImagePojo::new).toList());
    }
}
