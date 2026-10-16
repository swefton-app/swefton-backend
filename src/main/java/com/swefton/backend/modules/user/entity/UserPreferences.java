package com.swefton.backend.modules.user.entity;

import com.swefton.backend.modules.user.dto.UserPreferencesPojo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String timezone;

    @Column(name = "push_notifications", nullable = false)
    private boolean pushNotifications = true;

    @Column(name = "email_notifications", nullable = false)
    private boolean emailNotifications = true;

    @Column(name = "marketing_notifications", nullable = false)
    private boolean marketingNotifications = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public UserPreferences() {
    }

    public UserPreferences(UserPreferencesPojo pojo) {
        updateFrom(pojo);
    }

    public void updateFrom(UserPreferencesPojo pojo) {
        this.timezone = pojo.getTimezone();
        this.pushNotifications = pojo.isPushNotifications();
        this.emailNotifications = pojo.isEmailNotifications();
        this.marketingNotifications = pojo.isMarketingNotifications();
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

    @PreRemove
    public void preRemove() {
        deletedAt = LocalDateTime.now();
    }
}
