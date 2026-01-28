package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.ChannelType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ChannelResponse {
    private final UUID id;
    private final ChannelType type;
    private final String name;
    private final String description;
    private final List<UUID> participantIds;  // PRIVATE 채널인 경우 참여자 ID 목록
    private final Instant lastMessageAt;      // 가장 최근 메시지 시간
    private final Instant createdAt;
    private final Instant updatedAt;
}