package com.swefton.backend.modules.post.entity;

import com.swefton.backend.modules.image.entity.Image;
import com.swefton.backend.modules.video.entity.Video;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "post_media")
@Getter @Setter
public class PostMedia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "post_id", nullable = false) private Post post;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "image_id") private Image image;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "video_id") private Video video;
    @Column(nullable = false) private Integer position;
    @PrePersist @PreUpdate void validate() {
        if ((image == null) == (video == null)) throw new IllegalStateException("Post media must reference exactly one image or video");
    }
}
