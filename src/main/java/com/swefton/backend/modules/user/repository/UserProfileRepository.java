package com.swefton.backend.modules.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.swefton.backend.modules.user.entity.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    @Query("""
            SELECT profile
            FROM UserProfile profile
            WHERE profile.user.id = :userId
            AND profile.deletedAt IS NULL
            """)
    Optional<UserProfile> findByUserId(@Param("userId") Long userId);
}
