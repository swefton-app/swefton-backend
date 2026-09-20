package com.swefton.backend.modules.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.swefton.backend.modules.user.entity.UserRating;

public interface UserRatingRepository extends JpaRepository<UserRating, Long> {

    Optional<UserRating> findByUserIdAndFacilityId(Long userId, Long facilityId);

    Optional<UserRating> findByUserIdAndTrainerId(Long userId, Long trainerId);

    List<UserRating> findAllByFacilityIdOrderByCreatedAtDesc(Long facilityId);

    List<UserRating> findAllByTrainerIdOrderByCreatedAtDesc(Long trainerId);

    long countByFacilityId(Long facilityId);

    long countByTrainerId(Long trainerId);

    @Query("select avg(r.rating) from UserRating r where r.facility.id = :facilityId")
    Double findAverageByFacilityId(@Param("facilityId") Long facilityId);

    @Query("select avg(r.rating) from UserRating r where r.trainer.id = :trainerId")
    Double findAverageByTrainerId(@Param("trainerId") Long trainerId);
}
