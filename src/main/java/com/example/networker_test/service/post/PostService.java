package com.example.networker_test.service.post;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.networker_test.domain.comment.Comment;
import com.example.networker_test.domain.post.Post;
import com.example.networker_test.domain.user.User;
import com.example.networker_test.repository.post.PostRepository;
import com.example.networker_test.repository.user.UserRepository;
import jakarta.persistence.criteria.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.example.networker_test.exception.DataNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {
	private final PostRepository postRepository;
	private final UserRepository userRepository;

	public List<Post> getList(){
		return this.postRepository.findAll(); // 등록된 게시물 전체 조회
	}

	public Post getPost(Integer id) {
		Optional<Post> post = this.postRepository.findById(id); // id 유무에 따라 페이지 연결
		if (post.isPresent()) {
			return post.get(); // 존재|해당 페이지 연결
		} else {
			throw new DataNotFoundException("E:POST NOT FOUND"); // 존재하지 않는 id|404 pagenotfound 연결 및 E:POST NOT FOUND 남김(상위 디렉토리의 클래스로 연결)
		}
	}

	public Page<Post> getList(int page, String kw) {
		List<Sort.Order> sorts = new ArrayList<>();
		sorts.add(Sort.Order.desc("createDate"));
		Pageable pageable = PageRequest.of(page, 30, Sort.by(sorts));
		Specification<Post> spec = search(kw);// paging scope
		return this.postRepository.findAll(spec, pageable);
	}

	public void create(String subject, String content, HttpSession session) {
		User user = (User) session.getAttribute("user");
		if (user == null) {
			throw new RuntimeException("User not found in session");
		}

		Post post = new Post();
		post.setSubject(subject);
		post.setContent(content);
		post.setCreateDate(LocalDateTime.now());
		post.setAuthor(user); // User 객체를 통해 작성자 설정

		this.postRepository.save(post); // 게시물 생성 처리 연결(controller - entity)


	}

	public void update(Post post) {
		// 게시물 정보를 업데이트
		if (post.getId() == null || !postRepository.existsById(post.getId())) {
			throw new IllegalArgumentException("게시물을 찾을 수 없습니다.");
		}
		postRepository.save(post);
	}

	public void delete(Integer id) {
		if (!postRepository.existsById(id)) {
			throw new DataNotFoundException("게시물을 찾을 수 없습니다.");
		}
		postRepository.deleteById(id);
	}

	private Specification<Post> search(String kw) {
		return new Specification<>() {
			private static final long serialVersionUID = 1L;
			@Override
			public Predicate toPredicate(Root<Post> q, CriteriaQuery<?> query, CriteriaBuilder cb) {
				query.distinct(true);
				Join<Post, User> u1 = q.join("author", JoinType.LEFT);
				Join<Post, Comment> a = q.join("commentList", JoinType.LEFT);
				Join<Comment, User> u2 = a.join("author", JoinType.LEFT);
				return cb.or(cb.like(q.get("subject"), "%" + kw + "%"), // 제목
						cb.like(q.get("content"), "%" + kw + "%"),      // 내용
						cb.like(u1.get("nickname"), "%" + kw + "%"),    // 질문 작성자
						cb.like(a.get("content"), "%" + kw + "%"),      // 답변 내용
						cb.like(u2.get("nickname"), "%" + kw + "%"));   // 답변 작성자
			}
		};
	}
}


// 게시물 수정 메서드








