package com.sprint.mission.discodeit.entity;

import lombok.Getter;
import java.io.Serial;
import java.time.Instant;
import java.util.UUID;

@Getter
public class UserStatus extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID userId;          // 사용자 ID
    private Instant lastActiveAt;       // 마지막 접속 시간

    public UserStatus(UUID userId, Instant lastActiveAt) {
        super();
        this.userId = userId;
        this.lastActiveAt = lastActiveAt;
    }

    public void update(Instant lastActiveAt) {
        if (lastActiveAt != null) {
            this.lastActiveAt = lastActiveAt;
        }
        updateTimeStamp();
    }

    /**
     * 현재 온라인 상태인지 확인
     * 마지막 접속 시간이 현재 시간으로부터 5분 이내이면 온라인으로 간주
     */
    public boolean isOnline() {
        if (lastActiveAt == null) {
            return false;
        }
        Instant fiveMinutesAgo = Instant.now().minusSeconds(5 * 60);
        return lastActiveAt.isAfter(fiveMinutesAgo);
    }
}