package com.swefton.backend.modules.user.dto;

import com.swefton.backend.modules.user.entity.UserAddress;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class UserAddressPojo {

    private Long id;
    private Long userId;
    @NotBlank
    private String addressLine;
    @NotBlank
    private String city;
    private String state;
    private String postalCode;
    @NotBlank
    private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public UserAddressPojo() {
    }

    public UserAddressPojo(UserAddress entity) {
        this.id = entity.getId();
        this.userId = entity.getUser() != null ? entity.getUser().getId() : null;
        this.addressLine = entity.getAddressLine();
        this.city = entity.getCity();
        this.state = entity.getState();
        this.postalCode = entity.getPostalCode();
        this.country = entity.getCountry();
        this.latitude = entity.getLatitude();
        this.longitude = entity.getLongitude();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
        this.deletedAt = entity.getDeletedAt();
    }
}
