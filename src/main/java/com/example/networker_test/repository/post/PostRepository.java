package com.example.networker_test.repository.post;

import com.example.networker_test.domain.post.Post;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Integer> {

    Page<Post> findAll(Pageable pageable);
    
    Post findBySubject(String subject);
    Post findBySubjectAndContent(String subject, String content);
    List<Post> findBySubjectLike(String subject);
}
