package com.swefton.backend.modules.facility.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swefton.backend.modules.facility.entity.FacilityStaff;

public interface FacilityStaffRepository extends JpaRepository<FacilityStaff, Long> {

    List<FacilityStaff> findAllByFacilityIdAndActiveTrueOrderByCreatedAtAsc(Long facilityId);

    Optional<FacilityStaff> findByFacilityIdAndUserId(Long facilityId, Long userId);
}
