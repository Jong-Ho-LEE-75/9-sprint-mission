package com.sprint.mission.discodeit.dto.request;

import java.time.Instant;

/**
 * ========================================
 * 읽기 상태 수정 요청 DTO
 * ========================================
 *
 * 이 record는 읽기 상태 수정 시 클라이언트에서 서버로 전달되는 데이터를 담습니다.
 *
 * [업데이트 시점]
 * - 사용자가 채널에 입장할 때
 * - 사용자가 메시지를 읽을 때 (스크롤 등)
 * - 채널 화면이 포커스될 때
 *
 * [안 읽은 메시지 계산]
 * lastReadAt 이후에 생성된 메시지가 "안 읽은 메시지"입니다.
 * SELECT COUNT(*) FROM messages
 * WHERE channel_id = ? AND created_at > lastReadAt
 *
 * [사용 예시]
 * // 사용자가 채널에 입장했을 때
 * ReadStatusUpdateRequest request = new ReadStatusUpdateRequest(Instant.now());
 * readStatusService.update(readStatusId, request);
 *
 * @param lastReadAt 갱신할 마지막 읽은 시간 (보통 Instant.now())
 */
public record ReadStatusUpdateRequest(
        Instant lastReadAt
) {
}
