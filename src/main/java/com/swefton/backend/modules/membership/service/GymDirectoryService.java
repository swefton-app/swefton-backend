package com.swefton.backend.modules.membership.service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.modules.facility.entity.Facility;
import com.swefton.backend.modules.gym.entity.Gym;
import com.swefton.backend.modules.gym.enums.GymStatus;
import com.swefton.backend.modules.gym.repository.GymRepository;
import com.swefton.backend.modules.image.dto.ImagePojo;
import com.swefton.backend.modules.image.entity.Image;
import com.swefton.backend.modules.image.enums.ImageType;
import com.swefton.backend.modules.image.repository.ImageRepository;
import com.swefton.backend.modules.machine.repository.FacilityMachineRepository;
import com.swefton.backend.modules.membership.dto.GymDirectoryProfileResponse;
import com.swefton.backend.modules.membership.dto.GymDirectorySummaryResponse;
import com.swefton.backend.modules.membership.dto.GymMachineSummaryResponse;
import com.swefton.backend.modules.membership.dto.MembershipPlanResponse;
import com.swefton.backend.modules.membership.dto.OpeningHoursResponse;
import com.swefton.backend.modules.membership.enums.MembershipStatus;
import com.swefton.backend.modules.membership.repository.GymMembershipRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GymDirectoryService {

    private final GymRepository gymRepository;
    private final ImageRepository imageRepository;
    private final FacilityMachineRepository facilityMachineRepository;
    private final GymMembershipRepository gymMembershipRepository;
    private final MembershipPlanService membershipPlanService;
    private final GymMembershipService gymMembershipService;

    @Transactional
    public List<GymDirectorySummaryResponse> search(BigDecimal latitude, BigDecimal longitude, String query) {
        validateOptionalCoordinates(latitude, longitude);
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return gymRepository.findAll().stream()
                .filter(this::discoverable)
                .filter(gym -> matches(gym, normalizedQuery))
                .map(gym -> summary(gym, latitude, longitude))
                .sorted(Comparator.comparing((GymDirectorySummaryResponse gym) -> MembershipStatus.ACTIVE.equals(gym.membershipStatus())).reversed()
                        .thenComparing(GymDirectorySummaryResponse::checkedInByMe, Comparator.reverseOrder())
                        .thenComparing(
                        GymDirectorySummaryResponse::distanceKm,
                        Comparator.nullsLast(Double::compareTo))
                        .thenComparing(GymDirectorySummaryResponse::name, String.CASE_INSENSITIVE_ORDER))
                .limit(50)
                .toList();
    }

    @Transactional
    public GymDirectoryProfileResponse profile(Long gymId, BigDecimal latitude, BigDecimal longitude) {
        validateOptionalCoordinates(latitude, longitude);
        Gym gym = gymRepository.findById(gymId)
                .filter(this::discoverable)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gym not found"));
        Facility facility = gym.getFacility();
        List<Image> images = imageRepository.findAllByFacilityIdAndDeletedAtIsNullOrderByPositionAscCreatedAtDesc(facility.getId());
        return new GymDirectoryProfileResponse(
                gym.getId(),
                facility.getId(),
                facility.getName(),
                facility.getDescription(),
                address(facility),
                facility.getCity(),
                facility.getCountry(),
                facility.getLatitude(),
                facility.getLongitude(),
                latitude == null ? null : distance(latitude, longitude, facility.getLatitude(), facility.getLongitude()),
                facility.getPhoneNumber(),
                facility.getWebsiteUrl(),
                firstImage(images, ImageType.LOGO),
                firstImage(images, ImageType.COVER),
                images.stream().filter(image -> ImageType.GALLERY.equals(image.getType())).map(ImagePojo::new).toList(),
                gymMembershipRepository.countByFacilityIdAndStatusAndCheckedInTrue(facility.getId(), MembershipStatus.ACTIVE),
                hours(gym),
                facilityMachineRepository.findAllByFacilityIdOrderByMachineNameAsc(facility.getId()).stream()
                        .map(machine -> new GymMachineSummaryResponse(
                                machine.getMachine().getName(), machine.getQuantity(), machine.getStatus().getLabel()))
                        .toList(),
                membershipPlanService.activePlans(facility).stream().map(membershipPlanService::toResponse).toList(),
                gymMembershipService.currentFor(facility.getId()));
    }

    private GymDirectorySummaryResponse summary(Gym gym, BigDecimal latitude, BigDecimal longitude) {
        Facility facility = gym.getFacility();
        List<Image> images = imageRepository.findAllByFacilityIdAndDeletedAtIsNullOrderByPositionAscCreatedAtDesc(facility.getId());
        var membership = gymMembershipService.currentFor(facility.getId());
        return new GymDirectorySummaryResponse(
                gym.getId(),
                facility.getId(),
                facility.getName(),
                address(facility),
                facility.getCity(),
                facility.getCountry(),
                facility.getLatitude(),
                facility.getLongitude(),
                latitude == null ? null : distance(latitude, longitude, facility.getLatitude(), facility.getLongitude()),
                firstImage(images, ImageType.LOGO),
                gymMembershipRepository.countByFacilityIdAndStatusAndCheckedInTrue(facility.getId(), MembershipStatus.ACTIVE),
                membership != null && membership.checkedIn(),
                membership == null ? null : membership.status());
    }

    private boolean discoverable(Gym gym) {
        return gym.getFacility().getDeletedAt() == null
                && !GymStatus.INACTIVE.equals(gym.getStatus())
                && gym.getFacility().getLatitude() != null
                && gym.getFacility().getLongitude() != null;
    }

    private boolean matches(Gym gym, String query) {
        if (query.isBlank()) return true;
        Facility facility = gym.getFacility();
        return contains(facility.getName(), query)
                || contains(facility.getCity(), query)
                || contains(facility.getCountry(), query)
                || contains(facility.getFormattedAddress(), query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String address(Facility facility) {
        if (facility.getFormattedAddress() != null && !facility.getFormattedAddress().isBlank()) {
            return facility.getFormattedAddress();
        }
        return String.join(", ", List.of(facility.getAddressLine(), facility.getCity(), facility.getCountry()));
    }

    private ImagePojo firstImage(List<Image> images, String type) {
        return images.stream().filter(image -> type.equals(image.getType())).findFirst().map(ImagePojo::new).orElse(null);
    }

    private List<OpeningHoursResponse> hours(Gym gym) {
        if (gym.isOpen24Hours()) {
            return List.of(new OpeningHoursResponse("Every day", null, null, true));
        }
        LocalTime weekdayOpen = gym.getWeekdayOpenTime() == null ? LocalTime.of(6, 0) : gym.getWeekdayOpenTime();
        LocalTime weekdayClose = gym.getWeekdayCloseTime() == null ? LocalTime.of(22, 0) : gym.getWeekdayCloseTime();
        LocalTime weekendOpen = gym.getWeekendOpenTime() == null ? LocalTime.of(8, 0) : gym.getWeekendOpenTime();
        LocalTime weekendClose = gym.getWeekendCloseTime() == null ? LocalTime.of(20, 0) : gym.getWeekendCloseTime();
        return List.of(
                new OpeningHoursResponse("Monday – Friday", weekdayOpen, weekdayClose, false),
                new OpeningHoursResponse("Saturday – Sunday", weekendOpen, weekendClose, false));
    }

    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null
                || latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid location is required to search for gyms");
        }
    }

    private void validateOptionalCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null && longitude == null) return;
        validateCoordinates(latitude, longitude);
    }

    private double distance(BigDecimal fromLat, BigDecimal fromLon, BigDecimal toLat, BigDecimal toLon) {
        double earthRadiusKm = 6371.0088;
        double lat1 = Math.toRadians(fromLat.doubleValue());
        double lat2 = Math.toRadians(toLat.doubleValue());
        double deltaLat = lat2 - lat1;
        double deltaLon = Math.toRadians(toLon.doubleValue() - fromLon.doubleValue());
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        return Math.round((earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))) * 10.0) / 10.0;
    }
}
