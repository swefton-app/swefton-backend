package com.swefton.backend.modules.membership.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.swefton.backend.modules.facility.entity.Facility;
import com.swefton.backend.modules.membership.enums.MembershipStatus;
import com.swefton.backend.modules.payment.entity.Payment;
import com.swefton.backend.modules.user.entity.User;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "gym_membership")
@Getter
@Setter
public class GymMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_gym_membership_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", nullable = false, foreignKey = @ForeignKey(name = "fk_gym_membership_facility"))
    private Facility facility;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false, foreignKey = @ForeignKey(name = "fk_gym_membership_plan"))
    private MembershipPlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", foreignKey = @ForeignKey(name = "fk_gym_membership_payment"))
    private Payment payment;

    @Column(nullable = false, length = 20)
    private String status = MembershipStatus.PENDING;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;

    @Column(name = "starts_on")
    private LocalDate startsOn;

    @Column(name = "ends_on")
    private LocalDate endsOn;

    @Column(name = "week_started_on")
    private LocalDate weekStartedOn;

    @Column(name = "entries_this_week", nullable = false)
    private int entriesThisWeek;

    @Column(name = "checked_in", nullable = false)
    private boolean checkedIn;

    @PrePersist
    void prePersist() {
        if (requestedAt == null) requestedAt = LocalDateTime.now();
        if (status == null) status = MembershipStatus.PENDING;
        if (statusChangedAt == null) statusChangedAt = requestedAt;
    }
}
