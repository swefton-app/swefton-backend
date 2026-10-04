package com.swefton.backend.modules.machine.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swefton.backend.modules.machine.entity.Machine;

public interface MachineRepository extends JpaRepository<Machine, Long> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);
}
