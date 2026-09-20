package com.swefton.backend.modules.gym.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swefton.backend.modules.gym.entity.Gym;

public interface GymRepository extends JpaRepository<Gym, Long> {

    boolean existsByFacilityOwnerIdAndFacilityNameIgnoreCase(Long ownerId, String name);

    Optional<Gym> findByIdAndFacilityOwnerId(Long id, Long ownerId);

    Optional<Gym> findByFacilityIdAndFacilityOwnerId(Long facilityId, Long ownerId);

    List<Gym> findAllByFacilityOwnerIdOrderByCreatedAtAsc(Long ownerId);
}
