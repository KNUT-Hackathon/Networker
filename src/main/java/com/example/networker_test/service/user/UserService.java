package com.example.networker_test.service.user;

import com.example.networker_test.domain.user.User;
import com.example.networker_test.repository.user.UserRepository;
import com.example.networker_test.dto.user.reponese.UserResponse;
import com.example.networker_test.dto.user.request.UserCreateRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional
    public ResponseEntity<String> signupUser(UserCreateRequest request) {
        logger.info("Received signup request: nickname={}, email={}, nationality={}, password={}",
                request.getNickname(), request.getEmail(), request.getNationality(), request.getPassword());

        try {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                logger.warn("Email {} is already registered", request.getEmail());
                return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 등록된 아이디입니다.");
            }

            if (userRepository.findByNickname(request.getNickname()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("이미 등록된 닉네임입니다.");
            }

            if (!isValidNickname(request.getNickname())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("닉네임은 2글자 이상 10글자 이하여야 합니다.");
            }

            if (!isValidPassword(request.getPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("비밀번호는 최소 8자 이상이어야 합니다.");
            }

            String encodedPassword = passwordEncoder.encode(request.getPassword());
            userRepository.save(new User(request.getNickname(), request.getEmail(), encodedPassword, request.getNationality()));

            return ResponseEntity.ok("회원가입이 성공적으로 완료되었습니다.");
        } catch (Exception e) {
            logger.error("Error occurred while signing up user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("회원가입에 실패했습니다. 다시 시도해주세요.");
        }
    }

    @Transactional
    public ResponseEntity<String> loginUser(UserCreateRequest request, HttpSession session) {
        logger.info("Received login request: email={}, password={}", request.getEmail(), request.getPassword());

        try {
            Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

            if (userOptional.isEmpty()) {
                logger.info("No user found with email: {}", request.getEmail());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"message\": \"아이디가 틀렸습니다. 다시 시도해주세요.\"}");
            }

            User user = userOptional.get();
            logger.info("User found with email: {}", user.getEmail());

            if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                logger.info("Password matches for email: {}", user.getEmail());

                session.setAttribute("user", user);
                return ResponseEntity.ok("{\"message\": \"로그인이 성공적으로 완료되었습니다.\"}");
            }

            logger.info("Password does not match for email: {}", user.getEmail());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"message\": \"비밀번호를 틀렸습니다.\"}");
        } catch (Exception e) {
            logger.error("Error occurred while login user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"message\": \"로그인에 실패했습니다. 다시 시도해주세요.\"}");
        }
    }

    @Transactional
    public ResponseEntity<String> logoutUser(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("{\"message\": \"로그아웃이 성공적으로 완료되었습니다.\"}");
    }

    public ResponseEntity<UserResponse> getUserProfile(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        return ResponseEntity.ok(new UserResponse(user.getId(), user.getNickname(), user.getEmail(), user.getNationality(), user.getPassword()));
    }

    @Transactional
    public ResponseEntity<String> deleteUser(UserCreateRequest request, HttpSession session) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"message\": \"로그인이 필요합니다.\"}");
        }

        try {
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"message\": \"비밀번호가 틀렸습니다.\"}");
            }
            userRepository.delete(user);
            session.invalidate();
            return ResponseEntity.ok("{\"success\": true, \"message\": \"회원탈퇴가 성공적으로 완료되었습니다.\"}");
        } catch (Exception e) {
            logger.error("Error occurred while deleting user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"message\": \"회원 탈퇴 중 오류가 발생했습니다. 다시 시도해주세요.\"}");
        }
    }

    @Transactional
    public ResponseEntity<String> updateNickname(@RequestBody UserCreateRequest request, HttpSession session) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"message\": \"로그인이 필요합니다.\"}");
        }

        try {
            if (userRepository.findByNickname(request.getNickname()).isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"message\": \"중복된 닉네임 입니다.\"}");
            }

            if (!isValidNickname(request.getNickname())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"message\": \"닉네임은 2글자 이상 10글자 이하여야 합니다.\"}");
            }
            user.setNickname(request.getNickname());
            userRepository.save(user);

            session.setAttribute("user", user);
            return ResponseEntity.ok("{\"success\": true, \"message\": \"성공적으로 닉네임이 변경되었습니다.\"}");
        } catch (Exception e) {
            logger.error("Error occurred while updating nickname", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"message\": \"닉네임 변경 중 오류가 생겼습니다.\"}");
        }
    }

    @Transactional
    public ResponseEntity<String> updatePassword(@RequestBody Map<String, String> passwordData, HttpSession session) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"message\": \"로그인이 필요합니다.\"}");
        }

        String currentPassword = passwordData.get("currentPassword");
        String newPassword = passwordData.get("newPassword");

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"message\": \"현재 비밀번호가 틀렸습니다.\"}");
        }

        if (!isValidPassword(newPassword)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"message\": \"비밀번호는 최소 8자 이상이어야 합니다.\"}");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok("{\"success\": true, \"message\": \"비밀번호가 성공적으로 변경되었습니다.\"}");
    }

    @Transactional
    public ResponseEntity<String> updateEmail(@RequestBody UserCreateRequest request, HttpSession session) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"message\": \"로그인이 필요합니다.\"}");
        }

        try {
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"message\": \"비밀번호가 일치하지 않습니다.\"}");
            }

            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("{\"message\": \"이미 등록된 이메일입니다.\"}");
            }

            user.setEmail(request.getEmail());
            userRepository.save(user);

            session.setAttribute("user", user);
            return ResponseEntity.ok("{\"success\": true, \"message\": \"이메일이 성공적으로 변경되었습니다.\"}");
        } catch (Exception e) {
            logger.error("Error occurred while updating email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"message\": \"이메일 변경 중 오류가 발생했습니다.\"}");
        }
    }

    @Transactional
    public List<UserResponse> getUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserResponse(user.getId(), user.getNickname(), user.getEmail(), user.getNationality(), user.getPassword()))
                .collect(Collectors.toList());
    }

    @Transactional
    public ResponseEntity<Map<String, Object>> checkSession(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            return ResponseEntity.ok(Map.of("loggedIn", true, "userEmail", user.getEmail()));
        } else {
            return ResponseEntity.ok(Map.of("loggedIn", false));
        }
    }

    private boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }

    private boolean isValidNickname(String nickname) {
        return nickname != null && nickname.length() <= 10 && nickname.length() >= 2;
    }
}
