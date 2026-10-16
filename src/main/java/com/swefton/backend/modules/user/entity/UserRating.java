package com.swefton.backend.modules.user.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Check;

import com.swefton.backend.modules.facility.entity.Facility;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_ratings", indexes = {
        @Index(name = "idx_user_ratings_facility", columnList = "facility_id"),
        @Index(name = "idx_user_ratings_trainer", columnList = "trainer_id")
}, uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_user_ratings_user_facility",
                columnNames = { "user_id", "facility_id" }),
        @UniqueConstraint(
                name = "uk_user_ratings_user_trainer",
                columnNames = { "user_id", "trainer_id" })
})

@Getter
@Setter
public class UserRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_ratings_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", foreignKey = @ForeignKey(name = "fk_user_ratings_facility"))
    private Facility facility;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id", foreignKey = @ForeignKey(name = "fk_user_ratings_trainer"))
    private User trainer;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UserRating() {
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
