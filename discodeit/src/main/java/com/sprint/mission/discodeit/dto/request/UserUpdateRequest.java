package com.sprint.mission.discodeit.dto.request;

/**
 * ========================================
 * 사용자 정보 수정 요청 DTO
 * ========================================
 *
 * 이 record는 사용자 정보 수정 시 클라이언트에서 서버로 전달되는 데이터를 담습니다.
 *
 * [부분 업데이트(Partial Update) 지원]
 * 변경하고 싶은 필드만 값을 넣고, 나머지는 null로 전달하면 됩니다.
 * 서비스 레이어에서 null이 아닌 필드만 업데이트합니다.
 *
 * [사용 예시]
 * // 이메일만 변경
 * new UserUpdateRequest(null, "new@email.com", null)
 *
 * // 비밀번호만 변경
 * new UserUpdateRequest(null, null, "newPassword123")
 *
 * // 모든 정보 변경
 * new UserUpdateRequest("newUsername", "new@email.com", "newPassword")
 *
 * @param username 변경할 사용자명 (null이면 변경하지 않음)
 * @param email    변경할 이메일 주소 (null이면 변경하지 않음)
 * @param password 변경할 비밀번호 (null이면 변경하지 않음)
 */
public record UserUpdateRequest(
        String username,
        String email,
        String password
) {
}
