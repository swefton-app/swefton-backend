package com.swefton.backend.modules.membership.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "gym_visit")
@Getter
@Setter
public class GymVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membership_id", nullable = false, foreignKey = @ForeignKey(name = "fk_gym_visit_membership"))
    private GymMembership membership;

    @Column(name = "entered_at", nullable = false)
    private LocalDateTime enteredAt;

    @Column(name = "exited_at")
    private LocalDateTime exitedAt;
}
