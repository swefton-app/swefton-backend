package com.swefton.backend.modules.auth.entity;

import com.swefton.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
    name="user_auth_providers",
    uniqueConstraints={
        @UniqueConstraint(
            name="uk_auth_provider_subject",
            columnNames={"provider","provider_subject"}
        ),
        @UniqueConstraint(
            name="uk_auth_user_provider",
            columnNames={"user_id","provider"}
        )
    }
)
@Getter
@Setter
public class UserAuthProvider{

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="user_id",nullable=false)
    private User user;

    @Column(nullable=false,length=30)
    private String provider;

    @Column(name="provider_subject",nullable=false,length=255)
    private String providerSubject;

    @Column(name="provider_email",length=320)
    private String providerEmail;

    @Column(name="created_at",nullable=false)
    private LocalDateTime createdAt=LocalDateTime.now();

    @Column(name="updated_at",nullable=false)
    private LocalDateTime updatedAt=LocalDateTime.now();

    @PreUpdate
    public void preUpdate(){
        updatedAt=LocalDateTime.now();
    }
}
