package com.swefton.backend.modules.gym.entity;

import java.time.LocalDateTime;

import com.swefton.backend.modules.facility.entity.Facility;
import com.swefton.backend.modules.gym.enums.GymStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "gym")
@Getter
@Setter
public class Gym {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
    @JoinColumn(
            name = "facility_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_gym_facility"))
    private Facility facility;

    @Column(name = "gym_type", nullable = false, length = 40)
    private String type;

    @Column
    private Integer capacity;

    @Column(name = "open_24_hours", nullable = false)
    private boolean open24Hours;

    @Column(nullable = false, length = 30)
    private String status = GymStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Gym() {
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = GymStatus.DRAFT;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
