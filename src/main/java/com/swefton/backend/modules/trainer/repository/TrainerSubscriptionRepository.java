package com.swefton.backend.modules.trainer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swefton.backend.modules.trainer.entity.TrainerSubscription;

public interface TrainerSubscriptionRepository extends JpaRepository<TrainerSubscription, Long> {
    List<TrainerSubscription> findAllByClientIdOrderByRequestedAtDesc(Long clientId);
    List<TrainerSubscription> findAllByTrainerIdOrderByRequestedAtDesc(Long trainerId);
    boolean existsByClientIdAndTrainerIdAndStatusIn(Long clientId, Long trainerId, List<String> statuses);
}
