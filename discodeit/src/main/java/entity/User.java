package entity;

import java.util.UUID;

public class User {
    private final String id;
    private String username;
    private String email;
    private String nickname;
    private final Long createdAt;
    private Long updatedAt;

    public User(String username, String email, String nickname) {
        this.id = UUID.randomUUID().toString();
        this.username = username;
        this.email = email;
        this.nickname = nickname;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    // Getter 메소드들
    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    // Update 메소드
    public void update(String username, String email, String nickname) {
        this.username = username;
        this.email = email;
        this.nickname = nickname;
        this.updatedAt = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return String.format("사용자 [사용자명: %s, 이메일: %s, 닉네임: %s]",
                username,
                email,
                nickname);
    }
}