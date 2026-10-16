package com.swefton.backend.modules.machine.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swefton.backend.modules.machine.entity.FacilityMachine;

public interface FacilityMachineRepository extends JpaRepository<FacilityMachine, Long> {

    List<FacilityMachine> findAllByFacilityIdOrderByMachineNameAsc(Long facilityId);

    Optional<FacilityMachine> findByIdAndFacilityId(Long id, Long facilityId);

    boolean existsByFacilityIdAndMachineId(Long facilityId, Long machineId);

    boolean existsByMachineId(Long machineId);
}
