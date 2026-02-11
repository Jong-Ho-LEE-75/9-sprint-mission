package com.sprint.mission.discodeit.entity;

import lombok.Getter;
import java.io.Serial;
import java.time.Instant;
import java.util.UUID;

@Getter
public class ReadStatus extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID userId;      // 사용자 ID
    private final UUID channelId;   // 채널 ID
    private Instant lastReadAt;     // 마지막으로 메시지를 읽은 시간

    public ReadStatus(UUID userId, UUID channelId, Instant lastReadAt) {
        super();
        this.userId = userId;
        this.channelId = channelId;
        this.lastReadAt = lastReadAt;
    }

    public void update(Instant lastReadAt) {
        if (lastReadAt != null) {
            this.lastReadAt = lastReadAt;
        }
        updateTimeStamp();
    }
}