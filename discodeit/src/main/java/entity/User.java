package entity;

// UUID 사용을 위해 필요
import java.util.UUID;

public class User {
    private final UUID id;
    private final Long createdAt;
    private Long updatedAt;

    private String userName;
    private String email;
    private String status;

    public User(String userName, String email, String status) {
        this.id = UUID.randomUUID();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
        this.userName = userName;
        this.email = email;
        this.status = status; // online, offline, away 등)
    }

    // Getter - 정보를 가저오는 함수들

    public UUID getId() {
        return id;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public String getUserName() {
        return userName;
    }

    public String getEmail() {
        return email;
    }

    public String getStatus() {
        return status;
    }

    // Update - 정보를 수정하는 함수
    public void update(String userName, String email, String status) {
        this.userName = userName;
        this.email = email;
        this.status = status;
        this.updatedAt = System.currentTimeMillis(); // 수정시간 갱신
    }
}
