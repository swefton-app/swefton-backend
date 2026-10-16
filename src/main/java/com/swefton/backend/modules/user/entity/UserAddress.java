package com.swefton.backend.modules.user.entity;

import com.swefton.backend.modules.user.dto.UserAddressPojo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_addresses")
@Getter
@Setter
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "address_line")
    private String addressLine;

    @Column
    private String city;

    @Column
    private String state;

    @Column(name = "postal_code")
    private String postalCode;

    @Column
    private String country;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public UserAddress() {
    }

    public UserAddress(UserAddressPojo pojo) {
        updateFrom(pojo);
    }

    public void updateFrom(UserAddressPojo pojo) {
        this.addressLine = pojo.getAddressLine();
        this.city = pojo.getCity();
        this.state = pojo.getState();
        this.postalCode = pojo.getPostalCode();
        this.country = pojo.getCountry();
        this.latitude = pojo.getLatitude();
        this.longitude = pojo.getLongitude();
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
