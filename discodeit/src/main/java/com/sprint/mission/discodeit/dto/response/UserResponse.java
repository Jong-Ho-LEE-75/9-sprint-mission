package com.sprint.mission.discodeit.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * 사용자 응답 DTO
 * 사용자 정보를 반환할 때 사용합니다.
 *
 * @param id 사용자 ID
 * @param username 사용자명
 * @param email 이메일 주소
 * @param profileId 프로필 이미지 ID
 * @param isOnline 온라인 여부
 * @param createdAt 생성 시간
 * @param updatedAt 수정 시간
 */
public record UserResponse(
        UUID id,
        String username,
        String email,
        UUID profileId,
        boolean isOnline,
        Instant createdAt,
        Instant updatedAt
) {
}
