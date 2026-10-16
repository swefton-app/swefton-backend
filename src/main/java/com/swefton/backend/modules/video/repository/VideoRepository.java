package com.swefton.backend.modules.video.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swefton.backend.modules.video.entity.Video;

public interface VideoRepository extends JpaRepository<Video, Long> {

    Optional<Video> findByIdAndDeletedAtIsNull(Long id);

    Optional<Video> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    List<Video> findAllByIdInAndUserIdAndDeletedAtIsNull(Collection<Long> ids, Long userId);
}
