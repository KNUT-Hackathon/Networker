package com.example.networker_test.domain.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id = null;

    @Column(nullable = false, length = 10)
    private String nickname;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 100)
    private String nationality;

    protected User(){}

    public User(String nickname, String email, String password, String nationality) {
        if (nickname == null || email == null || password == null || nationality == null ||
                nickname.isBlank() || email.isBlank() || password.isBlank() || nationality.isBlank()) {
            throw new IllegalArgumentException("잘못 입력하셨습니다.");
        }
        this.nickname = nickname;
        this.email = email;
        this.password = password;
        this.nationality = nationality;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
