package com.sprint.mission.discodeit.dto.request;

import java.time.Instant;

/**
 * ========================================
 * 사용자 상태 수정 요청 DTO
 * ========================================
 *
 * 이 record는 사용자 상태(온라인/오프라인) 갱신 시
 * 클라이언트에서 서버로 전달되는 데이터를 담습니다.
 *
 * [업데이트 시점]
 * - 로그인 성공 시
 * - 메시지 전송 시
 * - 채널 전환 시
 * - 앱이 활성화될 때
 * - 주기적인 하트비트(Heartbeat)
 *
 * [온라인 상태 유지]
 * lastAccessAt이 현재 시간으로부터 5분 이내면 온라인으로 표시됩니다.
 * 따라서 온라인 상태를 유지하려면 5분마다 이 요청을 보내야 합니다.
 *
 * [사용 예시]
 * // 사용자 활동 시 마지막 접속 시간 갱신
 * UserStatusUpdateRequest request = new UserStatusUpdateRequest(Instant.now());
 * userStatusService.updateByUserId(userId, request);
 *
 * @param lastAccessAt 갱신할 마지막 접속 시간 (보통 Instant.now())
 */
public record UserStatusUpdateRequest(
        Instant lastAccessAt
) {
}
