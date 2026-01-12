package entity;

import java.util.UUID;

public class Message {
    private final String id;
    private String content;
    private String userId;
    private String channelId;
    private final Long createdAt;
    private Long updatedAt;

    public Message(String content, String userId, String channelId) {
        this.id = UUID.randomUUID().toString();
        this.content = content;
        this.userId = userId;
        this.channelId = channelId;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    // Getter 메소드들
    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getUserId() {
        return userId;
    }

    public String getChannelId() {
        return channelId;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    // Update 메소드
    public void update(String content, String userId, String channelId) {
        this.content = content;
        this.userId = userId;
        this.channelId = channelId;
        this.updatedAt = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return String.format("메시지 [내용: %s, 작성자ID: %s..., 채널ID: %s...]",
                content,
                userId.substring(0, 8),
                channelId.substring(0, 8));
    }
}