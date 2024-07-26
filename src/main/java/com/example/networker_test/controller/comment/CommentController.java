package com.example.networker_test.controller.comment;

import com.example.networker_test.service.comment.CommentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.networker_test.domain.comment.CommentForm;
import com.example.networker_test.domain.post.Post;
import com.example.networker_test.service.post.PostService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {
	
	private final PostService postService;//의존성 주입
	private final CommentService commentService;//의존성 주입
	
	@PostMapping("/create/{id}")//댓글 등록 처리
	public String createComment(Model model, @PathVariable("id") Integer id,
			@Valid CommentForm commentForm, BindingResult bindingResult){
		Post post = this.postService.getPost(id);
		if(bindingResult.hasErrors()) {
			model.addAttribute("post",post);
			return "post";
		}
		this.commentService.create(post,commentForm.getContent());
		return String.format("redirect:/post/detail/%s", id);//댓글 등록 후 등록 확인을 위한 reload 
	}
	

}
