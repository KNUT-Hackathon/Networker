package com.example.networker_test.service.post;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.networker_test.domain.post.Post;
import com.example.networker_test.repository.post.PostRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.networker_test.exception.DataNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {
	
	private final PostRepository postRepository;
	
	public List<Post> getList(){
		return this.postRepository.findAll();//등록된 게시물 전체 조회
	}

	public Post getPost(Integer id) {
		Optional<Post> post = this.postRepository.findById(id);//id 유무에 따라 페이지 연결
		if(post.isPresent()) {
			return post.get();//존재|해당 페이지 연결
		}else {
			throw new DataNotFoundException("E:POST NOT FOUND");//존재하지않는 id|404 pagenotfound 연결 및 E:POST NOT FOUND 남김(상위 디렉토리의 클래스로 연결)
		}
	}
	public void create(String subject, String content) {
		Post pos = new Post();
		pos.setSubject(subject);
		pos.setContent(content);
		pos.setCreateDate(LocalDateTime.now());
		this.postRepository.save(pos);
	//게시물 생성 처리 연결(controller - entity)
	}
    public Page<Post> getList(int page) {
        List<Sort.Order> sorts = new ArrayList<>();
        sorts.add(Sort.Order.desc("createDate"));
        Pageable pageable = PageRequest.of(page, 10, Sort.by(sorts));
        return this.postRepository.findAll(pageable);
    }
}
