package com.sprint.mission.discodeit.dto.response;

import com.sprint.mission.discodeit.entity.ChannelType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 채널 응답 DTO
 * 채널 정보를 반환할 때 사용합니다.
 *
 * @param id 채널 ID
 * @param type 채널 타입 (PUBLIC/PRIVATE)
 * @param name 채널 이름
 * @param description 채널 설명
 * @param participantIds 참여자 ID 목록
 * @param lastMessageAt 마지막 메시지 시간
 * @param createdAt 생성 시간
 * @param updatedAt 수정 시간
 */
public record ChannelResponse(
        UUID id,
        ChannelType type,
        String name,
        String description,
        List<UUID> participantIds,
        Instant lastMessageAt,
        Instant createdAt,
        Instant updatedAt
) {
}
