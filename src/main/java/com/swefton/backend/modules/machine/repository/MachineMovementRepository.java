package com.swefton.backend.modules.machine.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swefton.backend.modules.machine.entity.MachineMovement;

public interface MachineMovementRepository extends JpaRepository<MachineMovement, Long> {

    boolean existsByVideoId(Long videoId);

    boolean existsByVideoIdAndMachineIdNot(Long videoId, Long machineId);
}
