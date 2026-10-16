package com.swefton.backend.modules.membership.repository;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swefton.backend.modules.membership.entity.GymVisit;

public interface GymVisitRepository extends JpaRepository<GymVisit, Long> {
    Optional<GymVisit> findFirstByMembershipIdAndExitedAtIsNullOrderByEnteredAtDesc(Long membershipId);
    List<GymVisit> findAllByMembershipFacilityIdAndEnteredAtBetween(Long facilityId, LocalDateTime from, LocalDateTime to);
}
