package entity;

import java.util.UUID;

public class Channel {
    private final UUID id;
    private final Long createdAt;
    private Long updatedAt;

    private String userName;
    private String description;
    private String type;

    public Channel(String userName, String description, String type) {
        this.id = UUID.randomUUID();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
        this.userName = userName;
        this.description = description; // 설명, 서술
        this.type = type; // online, offline, away 등)
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

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    // Update - 정보를 수정하는 함수
    public void update(String userName, String description, String type) {
        this.userName = userName;
        this.description = description;
        this.type = type;
        this.updatedAt = System.currentTimeMillis(); // 수정시간 갱신
    }
}