package com.swefton.backend.modules.membership.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swefton.backend.modules.membership.entity.GymMembership;

public interface GymMembershipRepository extends JpaRepository<GymMembership, Long> {
    Optional<GymMembership> findFirstByUserIdAndFacilityIdOrderByRequestedAtDesc(Long userId, Long facilityId);
    Optional<GymMembership> findByIdAndFacilityId(Long id, Long facilityId);
    Optional<GymMembership> findByIdAndUserId(Long id, Long userId);
    List<GymMembership> findAllByUserIdOrderByRequestedAtDesc(Long userId);
    List<GymMembership> findAllByFacilityIdOrderByRequestedAtDesc(Long facilityId);
    List<GymMembership> findAllByFacilityId(Long facilityId);
    long countByFacilityIdAndStatus(Long facilityId, String status);
    long countByFacilityIdAndStatusAndCheckedInTrue(Long facilityId, String status);
    boolean existsByUserIdAndFacilityIdAndStatusIn(Long userId, Long facilityId, List<String> statuses);
}
