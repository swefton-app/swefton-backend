package com.swefton.backend.modules.user.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.swefton.backend.modules.facility.entity.Facility;
import com.swefton.backend.modules.facility.repository.FacilityRepository;
import com.swefton.backend.modules.user.dto.RatingOverviewResponse;
import com.swefton.backend.modules.user.dto.RatingRequest;
import com.swefton.backend.modules.user.dto.RatingResponse;
import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.user.entity.UserRating;
import com.swefton.backend.modules.user.enums.RoleCode;
import com.swefton.backend.modules.user.repository.UserRatingRepository;
import com.swefton.backend.modules.user.repository.UserRepository;
import com.swefton.backend.session.ISessionUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final UserRatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final FacilityRepository facilityRepository;
    private final ISessionUser sessionUser;

    @Transactional
    public RatingResponse rateFacility(Long facilityId, RatingRequest request) {
        User user = currentUser();
        Facility facility = activeFacility(facilityId);
        if (facility.getOwner().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot rate your own facility");
        }

        UserRating rating = ratingRepository.findByUserIdAndFacilityId(user.getId(), facilityId)
                .orElseGet(() -> newFacilityRating(user, facility));
        update(rating, request);
        return new RatingResponse(ratingRepository.saveAndFlush(rating));
    }

    @Transactional
    public RatingResponse rateTrainer(Long trainerId, RatingRequest request) {
        User user = currentUser();
        User trainer = activeTrainer(trainerId);
        if (trainer.getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot rate yourself");
        }

        UserRating rating = ratingRepository.findByUserIdAndTrainerId(user.getId(), trainerId)
                .orElseGet(() -> newTrainerRating(user, trainer));
        update(rating, request);
        return new RatingResponse(ratingRepository.saveAndFlush(rating));
    }

    @Transactional(readOnly = true)
    public RatingOverviewResponse getFacilityRatings(Long facilityId) {
        activeFacility(facilityId);
        List<RatingResponse> ratings = ratingRepository.findAllByFacilityIdOrderByCreatedAtDesc(facilityId)
                .stream()
                .map(RatingResponse::new)
                .toList();
        return new RatingOverviewResponse(
                average(ratingRepository.findAverageByFacilityId(facilityId)),
                ratings.size(),
                ratings);
    }

    @Transactional(readOnly = true)
    public RatingOverviewResponse getTrainerRatings(Long trainerId) {
        activeTrainer(trainerId);
        List<RatingResponse> ratings = ratingRepository.findAllByTrainerIdOrderByCreatedAtDesc(trainerId)
                .stream()
                .map(RatingResponse::new)
                .toList();
        return new RatingOverviewResponse(
                average(ratingRepository.findAverageByTrainerId(trainerId)),
                ratings.size(),
                ratings);
    }

    @Transactional
    public void deleteFacilityRating(Long facilityId) {
        deleteCurrentUsersRating(() -> ratingRepository.findByUserIdAndFacilityId(
                sessionUser.getUserId(), facilityId));
    }

    @Transactional
    public void deleteTrainerRating(Long trainerId) {
        deleteCurrentUsersRating(() -> ratingRepository.findByUserIdAndTrainerId(
                sessionUser.getUserId(), trainerId));
    }

    public BigDecimal facilityAverage(Long facilityId) {
        return average(ratingRepository.findAverageByFacilityId(facilityId));
    }

    public long facilityRatingCount(Long facilityId) {
        return ratingRepository.countByFacilityId(facilityId);
    }

    private void deleteCurrentUsersRating(Supplier<Optional<UserRating>> ratingSupplier) {
        UserRating rating = ratingSupplier.get()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rating not found"));
        ratingRepository.delete(rating);
    }

    private User currentUser() {
        return userRepository.findById(sessionUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Facility activeFacility(Long facilityId) {
        return facilityRepository.findByIdAndDeletedAtIsNull(facilityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facility not found"));
    }

    private User activeTrainer(Long trainerId) {
        User trainer = userRepository.findById(trainerId)
                .filter(User::isEnabled)
                .filter(user -> user.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trainer not found"));
        if (trainer.getRole() == null || !RoleCode.TRAINER.equals(trainer.getRole().getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a trainer");
        }
        return trainer;
    }

    private UserRating newFacilityRating(User user, Facility facility) {
        UserRating rating = new UserRating();
        rating.setUser(user);
        rating.setFacility(facility);
        return rating;
    }

    private UserRating newTrainerRating(User user, User trainer) {
        UserRating rating = new UserRating();
        rating.setUser(user);
        rating.setTrainer(trainer);
        return rating;
    }

    private void update(UserRating rating, RatingRequest request) {
        rating.setRating(request.rating());
        rating.setComment(trimToNull(request.comment()));
    }

    private BigDecimal average(Double average) {
        return average == null
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
