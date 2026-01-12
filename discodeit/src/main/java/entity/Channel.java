package entity;

import java.util.UUID;

public class Channel {
    private final String id;
    private String name;
    private String description;
    private String type;
    private final Long createdAt;
    private Long updatedAt;

    public Channel(String name, String description, String type) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.type = type;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    // Getter 메소드들
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    // Update 메소드
    public void update(String name, String description, String type) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.updatedAt = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return String.format("채널 [이름: %s, 설명: %s, 타입: %s]",
                name,
                description,
                type);
    }
}