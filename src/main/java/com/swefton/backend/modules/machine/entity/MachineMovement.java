package com.swefton.backend.modules.machine.entity;

import java.time.LocalDateTime;

import com.swefton.backend.modules.machine.enums.MachineMuscleGroup;
import com.swefton.backend.modules.video.entity.Video;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "machine_movements",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_machine_movements_machine_muscle_group",
                columnNames = { "machine_id", "muscle_group" }),
        indexes = @Index(
                name = "idx_machine_movements_machine_position",
                columnList = "machine_id, position"))
@Getter
@Setter
public class MachineMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "machine_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_machine_movements_machine"))
    private Machine machine;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "muscle_group", nullable = false, length = 30)
    private MachineMuscleGroup muscleGroup;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "video_id",
            unique = true,
            foreignKey = @ForeignKey(name = "fk_machine_movements_video"))
    private Video video;

    // Kept during the URL-to-upload migration so existing databases and rows remain readable.
    @Column(name = "video_url", length = 1000)
    private String legacyVideoUrl;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public MachineMovement() {
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

    public void assignVideo(Video video) {
        this.video = java.util.Objects.requireNonNull(video, "Movement video is required");
        this.legacyVideoUrl = "/api/v1/videos/" + video.getId();
    }

    public void clearVideo() {
        this.video = null;
        this.legacyVideoUrl = null;
    }
}
