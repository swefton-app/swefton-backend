package com.swefton.backend.modules.facility.entity;

import java.time.LocalDateTime;

import com.swefton.backend.modules.facility.enums.FacilityStaffInvitationStatus;
import com.swefton.backend.modules.user.entity.User;

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
@Table(name = "facility_staff_invitations", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_staff_invitation_facility_email",
                columnNames = { "facility_id", "email" }),
        @UniqueConstraint(
                name = "uk_staff_invitation_token_hash",
                columnNames = "token_hash")
})
@Getter
@Setter
public class FacilityStaffInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_staff_invitation_facility"))
    private Facility facility;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_staff_invitation_invited_by"))
    private User invitedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_staff_invitation_user"))
    private User user;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FacilityStaffInvitationStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "account_created_at")
    private LocalDateTime accountCreatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = FacilityStaffInvitationStatus.PENDING;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
