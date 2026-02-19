package com.sprint.mission.discodeit.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 메시지 응답 DTO
 * 메시지 정보를 반환할 때 사용합니다.
 *
 * @param id 메시지 ID
 * @param content 메시지 내용
 * @param channelId 채널 ID
 * @param authorId 작성자 ID
 * @param attachmentIds 첨부파일 ID 목록
 * @param createdAt 생성 시간
 * @param updatedAt 수정 시간
 */
public record MessageResponse(
        UUID id,
        String content,
        UUID channelId,
        UUID authorId,
        List<UUID> attachmentIds,
        Instant createdAt,
        Instant updatedAt
) {
}
