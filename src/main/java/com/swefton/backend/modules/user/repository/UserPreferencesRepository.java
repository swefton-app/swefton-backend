package com.swefton.backend.modules.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.swefton.backend.modules.user.entity.UserPreferences;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {

    @Query("""
            SELECT preferences
            FROM UserPreferences preferences
            WHERE preferences.user.id = :userId
            AND preferences.deletedAt IS NULL
            """)
    Optional<UserPreferences> findByUserId(@Param("userId") Long userId);
}

