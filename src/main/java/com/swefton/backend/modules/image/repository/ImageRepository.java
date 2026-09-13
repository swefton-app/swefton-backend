package com.swefton.backend.modules.image.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swefton.backend.modules.image.entity.Image;

public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findAllByUserIdAndDeletedAtIsNullOrderByPositionAscCreatedAtDesc(Long userId);

    Optional<Image> findByIdAndDeletedAtIsNull(Long id);

    Optional<Image> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);
}
