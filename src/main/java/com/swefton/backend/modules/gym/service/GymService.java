package com.swefton.backend.modules.gym.service;

import java.util.ArrayList;
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

import com.swefton.backend.modules.facility.dto.CreateFacilityRequest;
import com.swefton.backend.modules.facility.entity.Facility;
import com.swefton.backend.modules.facility.enums.FacilityCategory;
import com.swefton.backend.modules.gym.dto.GymResponse;
import com.swefton.backend.modules.gym.dto.UpdateGymLocationRequest;
import com.swefton.backend.modules.gym.entity.Gym;
import com.swefton.backend.modules.gym.enums.GymStatus;
import com.swefton.backend.modules.gym.enums.GymType;
import com.swefton.backend.modules.gym.repository.GymRepository;
import com.swefton.backend.modules.image.dto.ImagePojo;
import com.swefton.backend.modules.image.entity.Image;
import com.swefton.backend.modules.image.enums.ImageType;
import com.swefton.backend.modules.image.repository.ImageRepository;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.repository.UserRepository;
import com.swefton.backend.modules.user.service.RatingService;
import com.swefton.backend.session.ISessionUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GymService {

    private final GymRepository gymRepository;
    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final ISessionUser sessionUser;
    private final RatingService ratingService;

    @Transactional
    public GymResponse create(CreateFacilityRequest request) {
        User owner = userRepository.findById(sessionUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!owner.isOnboardingCompleted()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Complete business onboarding before adding another gym");
        }

        String category = validatedValue(request.category(), FacilityCategory::exists, "Facility category");
        String gymType = validatedValue(request.type(), GymType::exists, "Gym type");
        validateCoordinates(request);

        String name = request.name().trim();
        if (gymRepository.existsByFacilityOwnerIdAndFacilityNameIgnoreCase(owner.getId(), name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You already own a gym with this name");
        }

        Facility facility = new Facility();
        facility.setOwner(owner);
        facility.setName(name);
        facility.setCategory(category);
        facility.setDescription(trimToNull(request.description()));
        facility.setPublicEmail(normalizeEmail(request.publicEmail()));
        facility.setPhoneNumber(trimToNull(request.phoneNumber()));
        facility.setWebsiteUrl(trimToNull(request.websiteUrl()));
        facility.setAddressLine(request.addressLine().trim());
        facility.setCity(request.city().trim());
        facility.setState(trimToNull(request.state()));
        facility.setPostalCode(trimToNull(request.postalCode()));
        facility.setCountry(request.country().trim());
        facility.setFormattedAddress(normalizeFormattedAddress(request.formattedAddress()));
        facility.setLatitude(request.latitude());
        facility.setLongitude(request.longitude());
        facility.setLogoImage(resolveImage(owner.getId(), request.logoImageId(), ImageType.LOGO, "Logo"));
        facility.setCoverImage(resolveImage(owner.getId(), request.coverImageId(), ImageType.COVER, "Cover"));
        facility.setGalleryImages(resolveGalleryImages(owner.getId(), request.galleryImageIds()));

        Gym gym = new Gym();
        gym.setFacility(facility);
        gym.setType(gymType);
        gym.setCapacity(request.capacity());
        gym.setOpen24Hours(request.open24Hours());
        gym.setStatus(GymStatus.DRAFT);

        return toResponse(gymRepository.saveAndFlush(gym));
    }

    @Transactional(readOnly = true)
    public GymResponse findById(Long id) {
        Gym gym = gymRepository.findByIdAndFacilityOwnerId(id, sessionUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gym not found"));
        return toResponse(gym);
    }

    @Transactional(readOnly = true)
    public GymResponse findByFacilityId(Long facilityId) {
        Gym gym = gymRepository.findByFacilityIdAndFacilityOwnerId(facilityId, sessionUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gym not found"));
        return toResponse(gym);
    }

    @Transactional
    public GymResponse updateLocation(Long id, UpdateGymLocationRequest request) {
        Gym gym = gymRepository.findByIdAndFacilityOwnerId(id, sessionUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gym not found"));

        Facility facility = gym.getFacility();
        facility.setAddressLine(request.addressLine().trim());
        facility.setCity(request.city().trim());
        facility.setState(trimToNull(request.state()));
        facility.setPostalCode(trimToNull(request.postalCode()));
        facility.setCountry(request.country().trim());
        facility.setFormattedAddress(normalizeFormattedAddress(request.formattedAddress()));
        facility.setLatitude(request.latitude());
        facility.setLongitude(request.longitude());

        return toResponse(gymRepository.saveAndFlush(gym));
    }

    @Transactional(readOnly = true)
    public List<GymResponse> findAllForCurrentOwner() {
        return gymRepository.findAllByFacilityOwnerIdOrderByCreatedAtAsc(sessionUser.getUserId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private GymResponse toResponse(Gym gym) {
        Facility facility = gym.getFacility();
        return new GymResponse(
                gym.getId(),
                facility.getId(),
                facility.getOwner().getId(),
                facility.getCategory(),
                facility.getName(),
                facility.getDescription(),
                facility.getPublicEmail(),
                facility.getPhoneNumber(),
                facility.getWebsiteUrl(),
                facility.getAddressLine(),
                facility.getCity(),
                facility.getState(),
                facility.getPostalCode(),
                facility.getCountry(),
                facility.getFormattedAddress(),
                facility.getLatitude(),
                facility.getLongitude(),
                gym.getType(),
                gym.getCapacity(),
                gym.isOpen24Hours(),
                gym.getStatus(),
                toImageResponse(facility.getLogoImage()),
                toImageResponse(facility.getCoverImage()),
                facility.getGalleryImages().stream().map(ImagePojo::new).toList(),
                ratingService.facilityAverage(facility.getId()),
                ratingService.facilityRatingCount(facility.getId()),
                gym.getCreatedAt());
    }

    private Image resolveImage(Long ownerId, Long imageId, String requiredType, String label) {
        if (imageId == null) {
            return null;
        }
        Image image = imageRepository.findByIdAndUserIdAndDeletedAtIsNull(imageId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        label + " image does not exist or is not owned by you"));
        requireImageType(image, requiredType, label);
        return image;
    }

    private List<Image> resolveGalleryImages(Long ownerId, List<Long> requestedIds) {
        if (requestedIds == null || requestedIds.isEmpty()) {
            return new ArrayList<>();
        }
        if (requestedIds.stream().anyMatch(id -> id == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gallery image IDs cannot be null");
        }

        Set<Long> uniqueIds = new HashSet<>(requestedIds);
        if (uniqueIds.size() != requestedIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gallery image IDs must be unique");
        }

        Map<Long, Image> imagesById = new HashMap<>();
        imageRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(uniqueIds, ownerId)
                .forEach(image -> imagesById.put(image.getId(), image));

        List<Image> images = new ArrayList<>();
        for (Long imageId : requestedIds) {
            Image image = imagesById.get(imageId);
            if (image == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Gallery image does not exist or is not owned by you: " + imageId);
            }
            requireImageType(image, ImageType.GALLERY, "Gallery");
            images.add(image);
        }
        return images;
    }

    private void requireImageType(Image image, String requiredType, String label) {
        if (!requiredType.equals(image.getType())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    label + " image must have type " + requiredType);
        }
    }

    private ImagePojo toImageResponse(Image image) {
        return image == null ? null : new ImagePojo(image);
    }

    private void validateCoordinates(CreateFacilityRequest request) {
        if (request.formattedAddress() == null || request.formattedAddress().isBlank()
                || request.latitude() == null || request.longitude() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Choose the exact facility location on the map");
        }
    }

    private String validatedValue(String value, ValueValidator validator, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!validator.exists(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is invalid");
        }
        return normalized;
    }

    private String normalizeEmail(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeFormattedAddress(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Choose the exact facility location on the map");
        }

        StringBuilder normalized = new StringBuilder();
        int partCount = 0;
        for (String part : value.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!normalized.isEmpty()) {
                normalized.append(", ");
            }
            normalized.append(trimmed);
            partCount++;
            if (partCount == 4) {
                break;
            }
        }
        return normalized.toString();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @FunctionalInterface
    private interface ValueValidator {
        boolean exists(String value);
    }
}
