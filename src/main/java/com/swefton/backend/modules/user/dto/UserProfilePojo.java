package com.swefton.backend.modules.user.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.swefton.backend.modules.user.entity.UserProfile;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfilePojo {

    private Long id;
    private Long userId;
    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;
    private String displayName;
    private LocalDate dateOfBirth;
    private String gender;
    private String bio;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String avatarUrl;
    @PositiveOrZero
    private Integer experience;

    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public UserProfilePojo() {
    }

    public UserProfilePojo(UserProfile entity) {
        this.id = entity.getId();
        this.userId = entity.getUser() != null ? entity.getUser().getId() : null;
        this.firstName = entity.getFirstName();
        this.lastName = entity.getLastName();
        this.displayName = entity.getDisplayName();
        this.dateOfBirth = entity.getDateOfBirth();
        this.gender = entity.getGender();
        this.bio = entity.getBio();
        this.experience = entity.getExperience();
        this.price = entity.getPrice();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
        this.deletedAt = entity.getDeletedAt();
    }
}

