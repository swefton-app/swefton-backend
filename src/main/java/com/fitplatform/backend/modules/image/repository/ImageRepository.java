package com.fitplatform.backend.modules.image.repository;

import com.fitplatform.backend.modules.image.entity.Image;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageRepository
        extends JpaRepository<Image, Long> {

}
