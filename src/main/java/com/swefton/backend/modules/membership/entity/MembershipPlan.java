package com.swefton.backend.modules.membership.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.swefton.backend.modules.facility.entity.Facility;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "membership_plan")
@Getter
@Setter
public class MembershipPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", nullable = false, foreignKey = @ForeignKey(name = "fk_membership_plan_facility"))
    private Facility facility;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "visits_per_week")
    private Integer visitsPerWeek;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency = "EUR";

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays = 30;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
