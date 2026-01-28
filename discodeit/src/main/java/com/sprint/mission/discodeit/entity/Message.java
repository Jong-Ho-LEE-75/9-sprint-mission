package com.sprint.mission.discodeit.entity;

import lombok.Getter;
import java.util.List;
import java.util.UUID;

@Getter
public class Message extends BaseEntity {
    private String content;
    private final UUID channelId;
    private final UUID authorId;
    private final List<UUID> attachmentIds;  // 첨부파일 ID 목록 (BinaryContent 참조)

    public Message(String content, UUID channelId, UUID authorId, List<UUID> attachmentIds) {
        super();
        this.content = content;
        this.channelId = channelId;
        this.authorId = authorId;
        this.attachmentIds = attachmentIds;
    }

    public void update(String content) {
        if (content != null) {
            this.content = content;
            updateTimeStamp();
        }
    }
}