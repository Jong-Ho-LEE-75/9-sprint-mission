package com.sprint.mission.discodeit.dto.request;

import java.time.Instant;
import java.util.UUID;

/**
 * ========================================
 * 사용자 상태 생성 요청 DTO
 * ========================================
 *
 * 이 record는 새 사용자 상태 생성 시 클라이언트에서 서버로 전달되는 데이터를 담습니다.
 *
 * [UserStatus가 생성되는 시점]
 * 회원가입 시 User와 함께 자동으로 생성됩니다.
 * UserService.create() 내부에서 UserStatus도 함께 생성합니다.
 *
 * [주의사항]
 * 한 User당 하나의 UserStatus만 존재할 수 있습니다.
 * 중복 생성 시도 시 IllegalArgumentException이 발생합니다.
 *
 * [사용 예시]
 * // 대부분 UserService에서 자동 생성하지만, 직접 생성할 경우:
 * UserStatusCreateRequest request = new UserStatusCreateRequest(
 *     userId,
 *     Instant.now()  // 현재 시간을 마지막 접속 시간으로 설정
 * );
 * UserStatus userStatus = userStatusService.create(request);
 *
 * @param userId       사용자 ID - 이 사용자의 상태
 * @param lastAccessAt 마지막 접속(활동) 시간
 */
public record UserStatusCreateRequest(
        UUID userId,
        Instant lastAccessAt
) {
}
