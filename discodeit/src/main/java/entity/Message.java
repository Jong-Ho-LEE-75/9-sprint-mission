package entity;

import java.util.UUID;

public class Message {
    private final UUID id;
    private final Long createdAt;
    private Long UpdatedAt;

    private UUID userId; // message 작성자
    private UUID channelId; // message 등록된 채널
    private String content; // message 내용

    public Message(UUID userId, UUID channelId, String content) {
        this.id = UUID.randomUUID();
        this.createdAt = System.currentTimeMillis();
        UpdatedAt = this.createdAt;
        this.userId = userId;
        this.channelId = channelId;
        this.content = content;
    }

    public UUID getId() {
        return id;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public Long getUpdatedAt() {
        return UpdatedAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getChannelId() {
        return channelId;
    }

    public String getContent() {
        return content;
    }

    // Update method
    public void update(String content) {
        this.content = content;
        this.UpdatedAt = System.currentTimeMillis();
    }
}
