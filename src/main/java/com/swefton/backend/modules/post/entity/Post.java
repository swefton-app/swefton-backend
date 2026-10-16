package com.swefton.backend.modules.post.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.swefton.backend.modules.facility.entity.Facility;
import com.swefton.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "posts", indexes = @Index(name = "idx_posts_feed", columnList = "deleted_at, created_at, id"))
@Getter @Setter
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "author_id", nullable = false) private User author;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "facility_id") private Facility facility;
    @Column(columnDefinition = "TEXT") private String text;
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true) @OrderBy("position ASC, id ASC") private List<PostMedia> media = new ArrayList<>();
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @Column(name = "deleted_at") private LocalDateTime deletedAt;
    @PrePersist void create() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    @PreUpdate void update() { updatedAt = LocalDateTime.now(); }
}
