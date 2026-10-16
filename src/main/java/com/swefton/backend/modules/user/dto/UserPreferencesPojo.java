package com.swefton.backend.modules.user.dto;

import java.time.LocalDateTime;

import com.swefton.backend.modules.user.entity.UserPreferences;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPreferencesPojo {

    private Long id;
    private Long userId;
    @NotBlank
    private String timezone;
    private boolean pushNotifications;
    private boolean emailNotifications;
    private boolean marketingNotifications;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public UserPreferencesPojo() {
    }

    public UserPreferencesPojo(UserPreferences entity) {
        this.id = entity.getId();
        this.userId = entity.getUser() != null ? entity.getUser().getId() : null;
        this.timezone = entity.getTimezone();
        this.pushNotifications = entity.isPushNotifications();
        this.emailNotifications = entity.isEmailNotifications();
        this.marketingNotifications = entity.isMarketingNotifications();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
        this.deletedAt = entity.getDeletedAt();
    }
}
