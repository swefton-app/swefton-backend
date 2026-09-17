package com.swefton.backend.modules.image.entity;


import java.time.LocalDateTime;

import com.swefton.backend.modules.user.entity.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="images")
@Getter
@Setter
public class Image{

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(
        name="user_id",
        nullable=false,
        foreignKey=@ForeignKey(name="fk_images_user")
    )
    private User user;

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
        if(createdAt==null){
            createdAt=LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate(){
        updatedAt=LocalDateTime.now();
    }
}
