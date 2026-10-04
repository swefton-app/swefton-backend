package com.swefton.backend.modules.gym.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.modules.facility.dto.CreateFacilityRequest;
import com.swefton.backend.modules.gym.dto.GymResponse;
import com.swefton.backend.modules.gym.dto.UpdateGymLocationRequest;
import com.swefton.backend.modules.facility.entity.Facility;
import com.swefton.backend.modules.facility.enums.FacilityCategory;
import com.swefton.backend.modules.gym.entity.Gym;
import com.swefton.backend.modules.gym.enums.GymStatus;
import com.swefton.backend.modules.gym.enums.GymType;
import com.swefton.backend.modules.gym.repository.GymRepository;
import com.swefton.backend.modules.image.entity.Image;
import com.swefton.backend.modules.image.enums.ImageType;
import com.swefton.backend.modules.image.repository.ImageRepository;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.repository.UserRepository;
import com.swefton.backend.modules.user.service.RatingService;
import com.swefton.backend.session.ISessionUser;

@ExtendWith(MockitoExtension.class)
class GymServiceTests {

    @Mock
    private GymRepository gymRepository;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ISessionUser sessionUser;

    @Mock
    private RatingService ratingService;

    private GymService gymService;

    @BeforeEach
    void setUp() {
        gymService = new GymService(
                gymRepository,
                imageRepository,
                userRepository,
                sessionUser,
                ratingService);
    }

    @Test
    void createPersistsFacilityAndGymForCurrentOwner() {
        User owner = new User();
        owner.setId(42L);
        owner.setOnboardingCompleted(true);
        CreateFacilityRequest request = request("  Power House  ");

        when(sessionUser.getUserId()).thenReturn(42L);
        when(userRepository.findById(42L)).thenReturn(Optional.of(owner));
        when(gymRepository.existsByFacilityOwnerIdAndFacilityNameIgnoreCase(42L, "Power House"))
                .thenReturn(false);
        when(gymRepository.saveAndFlush(any(Gym.class))).thenAnswer(invocation -> {
            Gym saved = invocation.getArgument(0);
            saved.setId(7L);
            saved.getFacility().setId(8L);
            saved.getFacility().prePersist();
            saved.prePersist();
            return saved;
        });

        GymResponse response = gymService.create(request);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.facilityId()).isEqualTo(8L);
        assertThat(response.ownerId()).isEqualTo(42L);
        assertThat(response.name()).isEqualTo("Power House");
        assertThat(response.publicEmail()).isEqualTo("hello@powerhouse.test");
        assertThat(response.status()).isEqualTo(GymStatus.DRAFT);

        ArgumentCaptor<Gym> captor = ArgumentCaptor.forClass(Gym.class);
        verify(gymRepository).saveAndFlush(captor.capture());
        Gym gym = captor.getValue();
        assertThat(gym.getFacility().getOwner()).isSameAs(owner);
        assertThat(gym.getFacility().getCategory()).isEqualTo("GYM");
        assertThat(gym.getFacility().getFormattedAddress())
                .isEqualTo("1 Main Street, Tirana, 1001, Albania");
        assertThat(gym.getFacility().getLatitude()).isEqualByComparingTo("41.3275000");
        assertThat(gym.getFacility().getLongitude()).isEqualByComparingTo("19.8187000");
        assertThat(gym.getType()).isEqualTo(GymType.COMMERCIAL);
    }

    @Test
    void createPersistsSelectedFacilityCategory() {
        User owner = new User();
        owner.setId(42L);
        owner.setOnboardingCompleted(true);
        CreateFacilityRequest request = request(
                "Aqua Center",
                FacilityCategory.SWIMMING,
                GymType.OTHER);

        when(sessionUser.getUserId()).thenReturn(42L);
        when(userRepository.findById(42L)).thenReturn(Optional.of(owner));
        when(gymRepository.existsByFacilityOwnerIdAndFacilityNameIgnoreCase(42L, "Aqua Center"))
                .thenReturn(false);
        when(gymRepository.saveAndFlush(any(Gym.class))).thenAnswer(invocation -> {
            Gym saved = invocation.getArgument(0);
            saved.setId(7L);
            saved.getFacility().setId(8L);
            saved.getFacility().prePersist();
            saved.prePersist();
            return saved;
        });

        GymResponse response = gymService.create(request);

        assertThat(response.category()).isEqualTo(FacilityCategory.SWIMMING);
        assertThat(response.type()).isEqualTo(GymType.OTHER);

        ArgumentCaptor<Gym> captor = ArgumentCaptor.forClass(Gym.class);
        verify(gymRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getFacility().getCategory())
                .isEqualTo(FacilityCategory.SWIMMING);
    }

    @Test
    void createTransfersSelectedImagesIntoTheFacilityCollection() {
        User owner = new User();
        owner.setId(42L);
        owner.setOnboardingCompleted(true);
        Image logo = image(owner, 11L, ImageType.LOGO, 0);
        Image gallery = image(owner, 12L, ImageType.GALLERY, 1);
        CreateFacilityRequest base = request("Power House");
        CreateFacilityRequest request = new CreateFacilityRequest(
                base.name(), base.category(), base.description(), base.publicEmail(), base.phoneNumber(),
                base.websiteUrl(), base.addressLine(), base.city(), base.state(), base.postalCode(),
                base.country(), base.formattedAddress(), base.latitude(), base.longitude(), base.type(),
                base.capacity(), base.open24Hours(), logo.getId(), null, List.of(gallery.getId()));

        when(sessionUser.getUserId()).thenReturn(42L);
        when(userRepository.findById(42L)).thenReturn(Optional.of(owner));
        when(gymRepository.existsByFacilityOwnerIdAndFacilityNameIgnoreCase(42L, "Power House"))
                .thenReturn(false);
        when(imageRepository.findByIdAndUserIdAndDeletedAtIsNull(11L, 42L))
                .thenReturn(Optional.of(logo));
        when(imageRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(java.util.Set.of(12L), 42L))
                .thenReturn(List.of(gallery));
        when(gymRepository.saveAndFlush(any(Gym.class))).thenAnswer(invocation -> {
            Gym saved = invocation.getArgument(0);
            saved.setId(7L);
            saved.getFacility().setId(8L);
            saved.getFacility().prePersist();
            saved.prePersist();
            return saved;
        });

        GymResponse response = gymService.create(request);

        assertThat(response.logoImage().getId()).isEqualTo(11L);
        assertThat(response.galleryImages()).singleElement().extracting(image -> image.getId()).isEqualTo(12L);
        assertThat(logo.getUser()).isNull();
        assertThat(logo.getFacility().getId()).isEqualTo(8L);
        assertThat(gallery.getUser()).isNull();
        assertThat(gallery.getFacility()).isSameAs(logo.getFacility());
    }

    @Test
    void createRejectsDuplicateGymNameForSameOwner() {
        User owner = new User();
        owner.setId(42L);
        owner.setOnboardingCompleted(true);

        when(sessionUser.getUserId()).thenReturn(42L);
        when(userRepository.findById(42L)).thenReturn(Optional.of(owner));
        when(gymRepository.existsByFacilityOwnerIdAndFacilityNameIgnoreCase(42L, "Power House"))
                .thenReturn(true);

        assertThatThrownBy(() -> gymService.create(request("Power House")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        verify(gymRepository, never()).saveAndFlush(any(Gym.class));
    }

    @Test
    void createRejectsGymTypeOutsideAllowedStringValues() {
        User owner = new User();
        owner.setId(42L);
        owner.setOnboardingCompleted(true);
        CreateFacilityRequest valid = request("Power House");
        CreateFacilityRequest invalid = new CreateFacilityRequest(
                valid.name(),
                valid.category(),
                valid.description(),
                valid.publicEmail(),
                valid.phoneNumber(),
                valid.websiteUrl(),
                valid.addressLine(),
                valid.city(),
                valid.state(),
                valid.postalCode(),
                valid.country(),
                valid.formattedAddress(),
                valid.latitude(),
                valid.longitude(),
                "NOT_A_GYM_TYPE",
                valid.capacity(),
                valid.open24Hours(),
                valid.logoImageId(),
                valid.coverImageId(),
                valid.galleryImageIds());

        when(sessionUser.getUserId()).thenReturn(42L);
        when(userRepository.findById(42L)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> gymService.create(invalid))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST")
                .hasMessageContaining("Gym type is invalid");

        verify(gymRepository, never()).saveAndFlush(any(Gym.class));
    }

    @Test
    void createRejectsAdditionalGymBeforeOwnerCompletesOnboarding() {
        User owner = new User();
        owner.setId(42L);

        when(sessionUser.getUserId()).thenReturn(42L);
        when(userRepository.findById(42L)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> gymService.create(request("Power House")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST")
                .hasMessageContaining("Complete business onboarding");

        verify(gymRepository, never()).saveAndFlush(any(Gym.class));
    }

    @Test
    void findAllReturnsOnlyCurrentOwnersGyms() {
        User owner = new User();
        owner.setId(42L);
        Facility facility = new Facility();
        facility.setId(8L);
        facility.setOwner(owner);
        facility.setName("Power House");
        facility.setCategory("GYM");
        facility.setAddressLine("1 Main Street");
        facility.setCity("Tirana");
        facility.setCountry("Albania");
        Gym gym = new Gym();
        gym.setId(7L);
        gym.setFacility(facility);
        gym.setType(GymType.COMMERCIAL);
        gym.setStatus(GymStatus.ACTIVE);

        when(sessionUser.getUserId()).thenReturn(42L);
        when(gymRepository.findAllByFacilityOwnerIdOrderByCreatedAtAsc(42L))
                .thenReturn(List.of(gym));

        List<GymResponse> response = gymService.findAllForCurrentOwner();

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().category()).isEqualTo("GYM");
        assertThat(response.getFirst().name()).isEqualTo("Power House");
        assertThat(response.getFirst().ownerId()).isEqualTo(42L);
    }

    @Test
    void updateLocationChangesOnlyCurrentOwnersFacilityLocation() {
        User owner = new User();
        owner.setId(42L);
        Facility facility = new Facility();
        facility.setId(8L);
        facility.setOwner(owner);
        facility.setName("Power House");
        facility.setCategory("GYM");
        facility.setAddressLine("Old Street");
        facility.setCity("Old City");
        facility.setCountry("Albania");
        Gym gym = new Gym();
        gym.setId(7L);
        gym.setFacility(facility);
        gym.setType(GymType.COMMERCIAL);
        gym.setStatus(GymStatus.DRAFT);
        UpdateGymLocationRequest request = new UpdateGymLocationRequest(
                "10 New Street", "Tirana", "Tirana County", "1001", "Albania",
                "Rruga Mikel Maruli, Astir, Mëzez, Kashar, Tirana Municipality, Tirana County, 1051, Albania",
                new BigDecimal("41.3300000"), new BigDecimal("19.8200000"));

        when(sessionUser.getUserId()).thenReturn(42L);
        when(gymRepository.findByIdAndFacilityOwnerId(7L, 42L)).thenReturn(Optional.of(gym));
        when(gymRepository.saveAndFlush(gym)).thenReturn(gym);

        GymResponse response = gymService.updateLocation(7L, request);

        assertThat(response.addressLine()).isEqualTo("10 New Street");
        assertThat(response.formattedAddress()).isEqualTo("Rruga Mikel Maruli, Astir, Mëzez, Kashar");
        assertThat(response.latitude()).isEqualByComparingTo("41.3300000");
        assertThat(response.longitude()).isEqualByComparingTo("19.8200000");
        verify(gymRepository).findByIdAndFacilityOwnerId(7L, 42L);
        verify(gymRepository).saveAndFlush(gym);
    }

    private CreateFacilityRequest request(String name) {
        return request(name, FacilityCategory.GYM, GymType.COMMERCIAL);
    }

    private CreateFacilityRequest request(String name, String category, String type) {
        return new CreateFacilityRequest(
                name,
                category,
                "Strength and conditioning gym",
                " Hello@PowerHouse.test ",
                "+355 69 123 4567",
                "https://powerhouse.test",
                "1 Main Street",
                "Tirana",
                null,
                "1001",
                "Albania",
                "1 Main Street, Tirana, 1001, Albania",
                new BigDecimal("41.3275000"),
                new BigDecimal("19.8187000"),
                type,
                250,
                false,
                null,
                null,
                java.util.List.of());
    }

    private Image image(User uploader, Long id, String type, int position) {
        Image image = new Image();
        image.setId(id);
        image.assignToUser(uploader);
        image.setType(type);
        image.setPosition(position);
        image.setFilePath("user-images/42/" + id + ".png");
        image.setOriginalName(id + ".png");
        image.setContentType("image/png");
        image.setFileSize(5L);
        return image;
    }

}
