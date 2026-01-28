package com.sprint.mission.discodeit.entity;

import lombok.Getter;
import java.util.UUID;

@Getter
public class Message extends BaseEntity {
    private String content;
    private final UUID channelId;
    private final UUID authorId;

    public Message(String content, UUID channelId, UUID authorId) {
        super(); // ID 생성
        this.content = content;
        this.channelId = channelId;
        this.authorId = authorId;
    }

    public void update(String content) {
        if (content != null) {
            this.content = content;
            updateTimeStamp();
        }
    }
}