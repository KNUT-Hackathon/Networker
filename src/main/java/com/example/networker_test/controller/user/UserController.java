package com.example.networker_test.controller.user;

import com.example.networker_test.dto.user.request.UserCreateRequest;
import com.example.networker_test.dto.user.reponese.UserResponse;
import com.example.networker_test.service.user.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signupUser(@RequestBody UserCreateRequest request) {
        return userService.signupUser(request);
    }

    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@RequestBody UserCreateRequest request, HttpSession session) {
        return userService.loginUser(request, session);
    }

    @GetMapping("/signup")
    public List<UserResponse> getUsers() {
        return userService.getUsers();
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logoutUser(HttpSession session) {
        return userService.logoutUser(session);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteUser(@RequestBody UserCreateRequest request, HttpSession session){
        return userService.deleteUser(request, session);
    }

    @PostMapping("/update-nickname")
    public ResponseEntity<String> updateNickname(@RequestBody UserCreateRequest request, HttpSession session){
        return userService.updateNickname(request, session);
    }

    @PostMapping("/update-password")
    public ResponseEntity<String> updatePassword(@RequestBody Map<String, String> passwordData, HttpSession session) {
        return userService.updatePassword(passwordData, session);
    }

    @PostMapping("/update-email")
    public ResponseEntity<String> updateEmail(@RequestBody UserCreateRequest request,HttpSession session) {
        return userService.updateEmail(request, session);
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getUserProfile(HttpSession session) {
        return userService.getUserProfile(session);
    }

    @GetMapping("/board")
    public ResponseEntity<Map<String, Object>> CheckSession(HttpSession session){
        return userService.checkSession(session);
    }


}