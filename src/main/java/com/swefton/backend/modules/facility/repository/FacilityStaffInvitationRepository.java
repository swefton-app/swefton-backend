package com.swefton.backend.modules.facility.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.swefton.backend.modules.facility.entity.FacilityStaffInvitation;
import com.swefton.backend.modules.facility.enums.FacilityStaffInvitationStatus;

public interface FacilityStaffInvitationRepository extends JpaRepository<FacilityStaffInvitation, Long> {

    @EntityGraph(attributePaths = { "facility", "invitedBy", "user" })
    Optional<FacilityStaffInvitation> findByTokenHash(String tokenHash);

    Optional<FacilityStaffInvitation> findByFacilityIdAndEmailIgnoreCase(Long facilityId, String email);

    boolean existsByEmailIgnoreCaseAndStatus(String email, FacilityStaffInvitationStatus status);

    @EntityGraph(attributePaths = { "facility", "user" })
    Optional<FacilityStaffInvitation> findFirstByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId,
            FacilityStaffInvitationStatus status);

    @EntityGraph(attributePaths = { "facility", "invitedBy", "user" })
    List<FacilityStaffInvitation> findAllByFacilityIdOrderByCreatedAtDesc(Long facilityId);
}
