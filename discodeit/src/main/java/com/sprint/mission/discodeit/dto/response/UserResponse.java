package com.sprint.mission.discodeit.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * ========================================
 * 사용자 응답 DTO
 * ========================================
 *
 * 이 record는 사용자 정보를 클라이언트에게 반환할 때 사용됩니다.
 *
 * [Response DTO를 사용하는 이유]
 * 1. 민감한 정보 제외: User 엔티티에는 password가 있지만,
 *    응답에서는 비밀번호를 절대 노출하면 안 됩니다!
 * 2. 추가 정보 포함: isOnline 같은 계산된 정보를 포함할 수 있습니다.
 * 3. API 스펙 고정: Entity 구조가 변경되어도 API 응답 형식은 유지 가능
 *
 * [User Entity vs UserResponse]
 * User 엔티티:       id, username, email, password, profileId, createdAt, updatedAt
 * UserResponse DTO:  id, username, email,          profileId, createdAt, updatedAt, isOnline
 *                                         ↑ password 제외    ↑ isOnline 추가 (계산된 값)
 *
 * [사용 예시]
 * // Service에서 UserResponse 생성
 * UserResponse response = new UserResponse(
 *     user.getId(),
 *     user.getUsername(),
 *     user.getEmail(),
 *     user.getProfileId(),
 *     userStatus.isOnline(),  // UserStatus에서 계산
 *     user.getCreatedAt(),
 *     user.getUpdatedAt()
 * );
 *
 * @param id        사용자 고유 식별자 (UUID)
 * @param username  사용자명
 * @param email     이메일 주소
 * @param profileId 프로필 이미지 ID (null일 수 있음)
 * @param isOnline  온라인 여부 (true: 온라인, false: 오프라인)
 * @param createdAt 계정 생성 시간
 * @param updatedAt 정보 수정 시간
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
