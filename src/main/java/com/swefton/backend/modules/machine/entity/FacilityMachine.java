package com.swefton.backend.modules.machine.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.swefton.backend.modules.facility.entity.Facility;
import com.swefton.backend.modules.machine.enums.FacilityMachineStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "facility_machine",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_facility_machine_facility_machine",
                columnNames = { "facility_id", "machine_id" }))
@Getter
@Setter
public class FacilityMachine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "facility_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_facility_machine_facility"))
    private Facility facility;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "machine_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_facility_machine_machine"))
    private Machine machine;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FacilityMachineStatus status;

    @Column(length = 1000)
    private String notes;

    @Column(name = "expected_arrival_date")
    private LocalDate expectedArrivalDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public FacilityMachine() {
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
