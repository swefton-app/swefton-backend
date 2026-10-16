package com.swefton.backend.modules.trainer.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swefton.backend.modules.trainer.entity.TrainerPackage;

public interface TrainerPackageRepository extends JpaRepository<TrainerPackage, Long> {
    List<TrainerPackage> findAllByTrainerIdOrderByCreatedAtDesc(Long trainerId);
    List<TrainerPackage> findAllByTrainerIdAndActiveTrueOrderByPriceAsc(Long trainerId);
    Optional<TrainerPackage> findByIdAndTrainerId(Long id, Long trainerId);
}
