package com.swefton.backend.modules.facility.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swefton.backend.modules.facility.entity.Facility;

public interface FacilityRepository extends JpaRepository<Facility, Long> {

    Optional<Facility> findByIdAndDeletedAtIsNull(Long id);

    Optional<Facility> findFirstByOwnerIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long ownerId);
}
