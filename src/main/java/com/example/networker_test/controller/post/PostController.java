package com.example.networker_test.controller.post;

import com.example.networker_test.CommonUtil;
import com.example.networker_test.domain.comment.CommentForm;
import com.example.networker_test.domain.post.Post;
import com.example.networker_test.domain.post.PostForm;
import com.example.networker_test.domain.user.User;
import com.example.networker_test.service.comment.CommentService;
import com.example.networker_test.service.post.PostRecommendationService;
import com.example.networker_test.service.post.PostService;
import com.example.networker_test.service.user.UserService; // UserService 추가
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpSession;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
@RequestMapping("/post")
@RequiredArgsConstructor

public class PostController {
	private final PostService postService;
	private final CommentService commentService;
	private final UserService userService;
	private final CommonUtil commonUtil;
	private final PostRecommendationService postRecommendationService;

	@Value("${file.upload-dir}")
	private String uploadDir;

	@GetMapping("/list")
	public String list(Model model, @RequestParam(value="page", defaultValue = "0") int page, @RequestParam(value = "kw", defaultValue = "") String kw) {
		if (page < 0) {
			page = 0; // 페이지 번호가 음수일 경우 0으로 설정
		}
		Page<Post> paging = this.postService.getList(page, kw);
		model.addAttribute("paging", paging);
		model.addAttribute("kw", kw);
		return "board";
	}


	@GetMapping("/detail/{id}")
	public String detail(Model model, @PathVariable("id") Integer id, CommentForm commentForm) {
		Post post = this.postService.getPost(id);
		model.addAttribute("post", post);
		model.addAttribute("postContent", commonUtil.markdown(post.getContent())); // Markdown 내용 변환
		return "post";
	}

	@GetMapping("/create")
	public String postCreate(PostForm postForm) {
		return "createpost";
	}

	@PostMapping("/create")
	public String createPost(@Valid PostForm postForm, BindingResult bindingResult,
							 @RequestParam(value="file", required = false) MultipartFile file,
							 HttpSession session) {
		if (bindingResult.hasErrors()) {

			return "createpost";
		}

		try {
			// 세션에서 로그인된 사용자 가져오기
			User user = (User) session.getAttribute("user");
			if (user == null) {
				return "redirect:/login"; // 로그인 페이지로 리다이렉트
			}

			// 파일이 업로드 되었는지 확인하고 처리
			if (file != null && !file.isEmpty()) {
				String fileName = file.getOriginalFilename();
				Path destinationPath = Paths.get(uploadDir).resolve(fileName);
				File destinationFile = destinationPath.toFile();

				// 파일 저장
				file.transferTo(destinationFile);
			}

			// 게시글 생성
			this.postService.create(postForm.getSubject(), postForm.getContent(), session);
			return "redirect:/post/list";
		} catch (IOException e) {
			e.printStackTrace();
			return "파일 업로드 실패";
		}
	}


	@PostMapping("/uploadImage")
	public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
		try {
			String fileName = file.getOriginalFilename();
			Path destinationPath = Paths.get(uploadDir).resolve(fileName);
			File destinationFile = destinationPath.toFile();

			// 파일 저장
			file.transferTo(destinationFile);

			// 파일의 URL 생성
			String fileUrl = "/post/files/" + fileName;
			return ResponseEntity.ok(fileUrl);
		} catch (IOException e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("파일 업로드 실패");
		}
	}

	@GetMapping("/files/{filename:.+}")
	public ResponseEntity<Resource> getFile(@PathVariable String filename) {
		try {
			Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
			Resource resource = new UrlResource(filePath.toUri());
			if (resource.exists() || resource.isReadable()) {
				return ResponseEntity.ok().body(resource);
			} else {
				throw new RuntimeException("파일을 찾을 수 없습니다.");
			}
		} catch (Exception e) {
			throw new RuntimeException("파일을 읽을 수 없습니다.");
		}
	}


	@PostMapping("/comment/{postId}")
	public String createComment(@PathVariable("postId") Integer postId,
								@RequestParam(value="content") String content,
								HttpSession session) {
		try {
			// 게시물 가져오기
			Post post = postService.getPost(postId);

			// 댓글 생성
			commentService.create(postId, content, session);

			return String.format("redirect:/post/detail/%s", postId);
		} catch (Exception e) {
			e.printStackTrace();
			return "댓글 등록 실패";
		}

	}

	@GetMapping("/post/modify/{id}")
	public String showModifyForm(@PathVariable("id") Integer id, Model model, HttpSession session) {
		// 게시물 조회
		Post post = postService.getPost(id);

		// 세션에서 사용자 정보 가져오기
		User loggedInUser = (User) session.getAttribute("user");
		if (loggedInUser == null || !loggedInUser.getNickname().equals(post.getAuthor().getNickname())) {
			return "redirect:/login"; // 로그인되지 않았거나 권한이 없는 경우 리다이렉트
		}

		// 게시물 정보를 폼에 맞게 설정
		PostForm postForm = new PostForm(null, null);
		postForm.setSubject(post.getSubject());
		postForm.setContent(post.getContent());

		model.addAttribute("postForm", postForm);
		model.addAttribute("postId", id);

		return "modifyPost"; // 수정 폼 템플릿
	}


	@GetMapping("/modify/{id}")
	public String modifyPostForm(@PathVariable("id") Integer id, Model model, HttpSession session) {
		Post post = postService.getPost(id);
		// 세션에서 사용자 정보 가져오기
		User loggedInUser = (User) session.getAttribute("user");
		if (loggedInUser == null || !loggedInUser.getNickname().equals(post.getAuthor().getNickname())) {
			return "redirect:/login"; // 권한이 없는 경우 로그인 페이지로 리다이렉트
		}
		model.addAttribute("postForm", new PostForm(post.getSubject(), post.getContent()));
		model.addAttribute("postId", id);
		return "modifypost"; // 수정 폼을 표시할 템플릿 이름
	}

	@PostMapping("/modify/{id}")
	public String modifyPost(@PathVariable("id") Integer id,
							 @Valid @ModelAttribute("postForm") PostForm postForm,
							 BindingResult bindingResult,
							 HttpSession session) {
		if (bindingResult.hasErrors()) {
			return "modifypost"; // 폼 검증 오류가 있는 경우 수정 폼을 다시 표시
		}

		Post post = postService.getPost(id);
		// 세션에서 사용자 정보 가져오기
		User loggedInUser = (User) session.getAttribute("user");
		if (loggedInUser == null || !loggedInUser.getNickname().equals(post.getAuthor().getNickname())) {
			return "redirect:/login"; // 로그인되지 않았거나 권한이 없는 경우 리다이렉트
		}

		post.setSubject(postForm.getSubject());
		post.setContent(postForm.getContent());
		postService.update(post);

		return "redirect:/post/detail/" + id; // 수정 후 상세 페이지로 리다이렉트
	}

	@PostMapping("/delete/{id}")
	public String deletePost(@PathVariable("id") Integer id, HttpSession session) {
		try {
			// 현재 사용자 정보를 가져옵니다.
			User currentUser = (User) session.getAttribute("user");
			if (currentUser == null) {
				return "redirect:/login"; // 로그인되지 않은 경우 로그인 페이지로 리다이렉트
			}

			// 게시물을 가져옵니다.
			Post post = postService.getPost(id);
			if (post.getAuthor() == null || !post.getAuthor().getId().equals(currentUser.getId())) {
				return "redirect:/post/list"; // 작성자가 아닌 경우 게시물 목록 페이지로 리다이렉트
			}

			// 게시물 삭제
			postService.delete(id);
			return "redirect:/post/list"; // 게시물 삭제 후 목록 페이지로 리다이렉트
		} catch (Exception e) {
			e.printStackTrace();
			return "게시물 삭제 실패";
		}
	}


	@PostMapping("/recommend/{id}")
	public String recommendPost(@PathVariable("id") Integer id, HttpSession session) {
		User currentUser = (User) session.getAttribute("user");
		if (currentUser == null) {
			return "redirect:/login"; // 로그인되지 않은 경우 로그인 페이지로 리다이렉트
		}

		try {
			postRecommendationService.recommendPost(id, currentUser);
			return "redirect:/post/detail/" + id; // 추천 후 게시물 상세 페이지로 리다이렉트
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return "redirect:/post/detail/" + id + "?error=recommendation"; // 이미 추천한 경우
		}
	}
}




