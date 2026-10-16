package com.swefton.backend.modules.post.entity;

import java.time.LocalDateTime;
import com.swefton.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "post_likes", uniqueConstraints = @UniqueConstraint(name = "uk_post_like", columnNames = {"post_id", "user_id"}))
@Getter @Setter
public class PostLike {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "post_id", nullable = false) private Post post;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @PrePersist void create() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
