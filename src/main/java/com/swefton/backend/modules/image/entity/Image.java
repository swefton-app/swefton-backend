package com.swefton.backend.modules.image.entity;


import java.time.LocalDateTime;

import com.swefton.backend.modules.user.entity.User;
import com.swefton.backend.modules.facility.entity.Facility;
import com.swefton.backend.modules.machine.entity.Machine;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "images",
    indexes = {
        @Index(name = "idx_images_user", columnList = "user_id, deleted_at, position"),
        @Index(name = "idx_images_facility", columnList = "facility_id, deleted_at, position"),
        @Index(name = "idx_images_machine", columnList = "machine_id, deleted_at, position")
    }
)
@Getter
@Setter
public class Image{

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "user_id",
        foreignKey = @ForeignKey(name = "fk_images_user")
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "facility_id",
        foreignKey = @ForeignKey(name = "fk_images_facility")
    )
    private Facility facility;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "machine_id",
        foreignKey = @ForeignKey(name = "fk_images_machine")
    )
    private Machine machine;

    @Column(name="type",nullable=false,length=30)
    private String type;

    @Column(name="file_path",nullable=false,unique=true,length=500)
    private String filePath;

    @Column(name="original_name",nullable=false,length=255)
    private String originalName;

    @Column(name="content_type",nullable=false,length=100)
    private String contentType;

    @Column(name="file_size",nullable=false)
    private Long fileSize;

    @Column(name="position")
    private Integer position;

    @Column(name="created_at",nullable=false)
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    @Column(name="deleted_at")
    private LocalDateTime deletedAt;

    public Image(){
    }

    @PrePersist
    public void prePersist(){
        requireExactlyOneOwner();
        if(createdAt==null){
            createdAt=LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate(){
        requireExactlyOneOwner();
        updatedAt=LocalDateTime.now();
    }

    public void assignToUser(User user) {
        this.user = java.util.Objects.requireNonNull(user, "Image user is required");
        this.facility = null;
        this.machine = null;
    }

    public void assignToFacility(Facility facility) {
        this.user = null;
        this.facility = java.util.Objects.requireNonNull(facility, "Image facility is required");
        this.machine = null;
    }

    public void assignToMachine(Machine machine) {
        this.user = null;
        this.facility = null;
        this.machine = java.util.Objects.requireNonNull(machine, "Image machine is required");
    }

    private void requireExactlyOneOwner() {
        int ownerCount = (user == null ? 0 : 1)
                + (facility == null ? 0 : 1)
                + (machine == null ? 0 : 1);
        if (ownerCount != 1) {
            throw new IllegalStateException("Image must belong to exactly one user, facility, or machine");
        }
    }
}
