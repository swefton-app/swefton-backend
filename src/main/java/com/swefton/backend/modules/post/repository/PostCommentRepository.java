package com.swefton.backend.modules.post.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.swefton.backend.modules.post.entity.PostComment;
public interface PostCommentRepository extends JpaRepository<PostComment, Long> {
    List<PostComment> findAllByPostIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(Long postId);
    long countByPostIdAndDeletedAtIsNull(Long postId);
}
