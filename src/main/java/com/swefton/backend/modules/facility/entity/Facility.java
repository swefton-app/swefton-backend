package com.swefton.backend.modules.facility.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.swefton.backend.modules.image.entity.Image;
import com.swefton.backend.modules.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "facility")
@Getter
@Setter
public class Facility {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "owner_id", nullable = false, foreignKey = @ForeignKey(name = "fk_facility_owner"))
        private User owner;

        @Column(nullable = false, length = 150)
        private String name;

        @Column(length = 40)
        private String category;

        @Column(columnDefinition = "TEXT")
        private String description;

        @Column(name = "public_email", length = 254)
        private String publicEmail;

        @Column(name = "phone_number", length = 30)
        private String phoneNumber;

        @Column(name = "website_url", length = 500)
        private String websiteUrl;

        @Column(name = "address_line", nullable = false, length = 255)
        private String addressLine;

        @Column(nullable = false, length = 100)
        private String city;

        @Column(length = 100)
        private String state;

        @Column(name = "postal_code", length = 20)
        private String postalCode;

        @Column(nullable = false, length = 100)
        private String country;

        @Column(name = "formatted_address", length = 500)
        private String formattedAddress;

        @Column(precision = 10, scale = 7)
        private BigDecimal latitude;

        @Column(precision = 10, scale = 7)
        private BigDecimal longitude;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "logo_image_id", foreignKey = @ForeignKey(name = "fk_facility_logo_image"))
        private Image logoImage;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "cover_image_id", foreignKey = @ForeignKey(name = "fk_facility_cover_image"))
        private Image coverImage;

        @ManyToMany(fetch = FetchType.LAZY)
        @JoinTable(name = "facility_gallery_images", joinColumns = @JoinColumn(name = "facility_id", foreignKey = @ForeignKey(name = "fk_facility_gallery_facility")), inverseJoinColumns = @JoinColumn(name = "image_id", foreignKey = @ForeignKey(name = "fk_facility_gallery_image")))
        @OrderColumn(name = "position")
        private List<Image> galleryImages = new ArrayList<>();

        @Column(name = "created_at", nullable = false, updatable = false)
        private LocalDateTime createdAt;

        @Column(name = "updated_at")
        private LocalDateTime updatedAt;

        @Column(name = "deleted_at")
        private LocalDateTime deletedAt;

        public Facility() {
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
