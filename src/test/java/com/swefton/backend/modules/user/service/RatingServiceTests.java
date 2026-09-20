package com.swefton.backend.modules.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.modules.facility.entity.Facility;
import com.swefton.backend.modules.facility.repository.FacilityRepository;
import com.swefton.backend.modules.user.dto.RatingOverviewResponse;
import com.swefton.backend.modules.user.dto.RatingRequest;
import com.swefton.backend.modules.user.dto.RatingResponse;
import com.swefton.backend.modules.user.entity.Role;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.entity.UserRating;
import com.swefton.backend.modules.user.enums.RoleCode;
import com.swefton.backend.modules.user.repository.UserRatingRepository;
import com.swefton.backend.modules.user.repository.UserRepository;
import com.swefton.backend.session.ISessionUser;

@ExtendWith(MockitoExtension.class)
class RatingServiceTests {

    @Mock
    private UserRatingRepository ratingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private ISessionUser sessionUser;

    private RatingService ratingService;

    @BeforeEach
    void setUp() {
        ratingService = new RatingService(ratingRepository, userRepository, facilityRepository, sessionUser);
    }

    @Test
    void rateFacilityCreatesOneRatingAndNormalizesTheComment() {
        User user = user(1L);
        Facility facility = facility(10L, user(2L));
        when(sessionUser.getUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(facilityRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(facility));
        when(ratingRepository.findByUserIdAndFacilityId(1L, 10L)).thenReturn(Optional.empty());
        when(ratingRepository.saveAndFlush(any(UserRating.class))).thenAnswer(invocation -> {
            UserRating saved = invocation.getArgument(0);
            saved.setId(100L);
            saved.prePersist();
            return saved;
        });

        RatingResponse response = ratingService.rateFacility(10L, new RatingRequest(5, "  Great gym  "));

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.facilityId()).isEqualTo(10L);
        assertThat(response.trainerId()).isNull();
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.comment()).isEqualTo("Great gym");
    }

    @Test
    void rateTrainerUpdatesTheExistingRating() {
        User user = user(1L);
        User trainer = trainer(20L);
        UserRating existing = new UserRating();
        existing.setId(101L);
        existing.setUser(user);
        existing.setTrainer(trainer);
        existing.setRating(3);
        when(sessionUser.getUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findById(20L)).thenReturn(Optional.of(trainer));
        when(ratingRepository.findByUserIdAndTrainerId(1L, 20L)).thenReturn(Optional.of(existing));
        when(ratingRepository.saveAndFlush(existing)).thenReturn(existing);

        RatingResponse response = ratingService.rateTrainer(20L, new RatingRequest(4, " "));

        assertThat(response.id()).isEqualTo(101L);
        assertThat(response.rating()).isEqualTo(4);
        assertThat(response.comment()).isNull();
        verify(ratingRepository).saveAndFlush(existing);
    }

    @Test
    void rateFacilityRejectsTheFacilityOwner() {
        User owner = user(1L);
        Facility facility = facility(10L, owner);
        when(sessionUser.getUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(facilityRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(facility));

        assertThatThrownBy(() -> ratingService.rateFacility(10L, new RatingRequest(5, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST")
                .hasMessageContaining("own facility");

        verify(ratingRepository, never()).saveAndFlush(any());
    }

    @Test
    void rateTrainerRequiresATrainerTarget() {
        User user = user(1L);
        User notTrainer = user(20L);
        Role role = new Role();
        role.setCode(RoleCode.USER);
        notTrainer.setRole(role);
        when(sessionUser.getUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findById(20L)).thenReturn(Optional.of(notTrainer));

        assertThatThrownBy(() -> ratingService.rateTrainer(20L, new RatingRequest(4, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST")
                .hasMessageContaining("not a trainer");

        verify(ratingRepository, never()).saveAndFlush(any());
    }

    @Test
    void facilityOverviewReturnsRoundedAverageCountAndComments() {
        Facility facility = facility(10L, user(2L));
        UserRating first = facilityRating(100L, user(1L), facility, 4, "Good");
        UserRating second = facilityRating(101L, user(3L), facility, 5, "Excellent");
        when(facilityRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(facility));
        when(ratingRepository.findAllByFacilityIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(second, first));
        when(ratingRepository.findAverageByFacilityId(10L)).thenReturn(4.5);

        RatingOverviewResponse response = ratingService.getFacilityRatings(10L);

        assertThat(response.averageRating()).isEqualByComparingTo("4.50");
        assertThat(response.ratingCount()).isEqualTo(2);
        assertThat(response.ratings()).extracting(RatingResponse::comment)
                .containsExactly("Excellent", "Good");
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setEnabled(true);
        return user;
    }

    private User trainer(Long id) {
        User trainer = user(id);
        Role role = new Role();
        role.setCode(RoleCode.TRAINER);
        trainer.setRole(role);
        return trainer;
    }

    private Facility facility(Long id, User owner) {
        Facility facility = new Facility();
        facility.setId(id);
        facility.setOwner(owner);
        return facility;
    }

    private UserRating facilityRating(
            Long id,
            User user,
            Facility facility,
            int score,
            String comment) {
        UserRating rating = new UserRating();
        rating.setId(id);
        rating.setUser(user);
        rating.setFacility(facility);
        rating.setRating(score);
        rating.setComment(comment);
        return rating;
    }
}
