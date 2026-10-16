package com.swefton.backend.modules.post.repository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.swefton.backend.modules.post.entity.PostLike;
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);
    long countByPostId(Long postId);
    List<PostLike> findAllByPostIdInAndUserId(Collection<Long> postIds, Long userId);
}
