package com.swefton.backend.modules.post.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.swefton.backend.modules.post.entity.Post;
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {}
