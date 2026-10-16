package com.swefton.backend.modules.membership.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import com.swefton.backend.modules.membership.entity.GymAccessPass;

public interface GymAccessPassRepository extends JpaRepository<GymAccessPass, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GymAccessPass> findByToken(String token);
}
