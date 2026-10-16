package com.swefton.backend.modules.membership.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swefton.backend.modules.membership.entity.MembershipPlan;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long> {
    List<MembershipPlan> findAllByFacilityIdAndDeletedAtIsNullAndActiveTrueOrderByDurationDaysAscVisitsPerWeekAsc(Long facilityId);
    List<MembershipPlan> findAllByFacilityIdAndDeletedAtIsNullOrderByDurationDaysAscVisitsPerWeekAsc(Long facilityId);
    Optional<MembershipPlan> findByIdAndFacilityIdAndDeletedAtIsNull(Long id, Long facilityId);
    long countByFacilityIdAndDeletedAtIsNull(Long facilityId);
}
