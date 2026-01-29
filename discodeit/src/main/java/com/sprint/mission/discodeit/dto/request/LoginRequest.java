package com.sprint.mission.discodeit.dto.request;

/**
 * ========================================
 * 로그인 요청 DTO
 * ========================================
 *
 * 이 record는 사용자 로그인 시 인증에 필요한 정보를 담습니다.
 *
 * [로그인 프로세스]
 * 1. 클라이언트가 username과 password를 포함한 LoginRequest 전송
 * 2. AuthService.login()에서 username으로 사용자 조회
 * 3. 비밀번호 일치 여부 확인
 * 4. 성공 시 UserResponse 반환, 실패 시 예외 발생
 *
 * [보안 주의사항]
 * 실제 프로덕션에서는:
 * - 비밀번호는 HTTPS로 암호화하여 전송
 * - 로그인 실패 시 "사용자명 또는 비밀번호가 잘못되었습니다" 같은
 *   모호한 메시지 사용 (어떤 것이 틀렸는지 알려주면 보안 취약)
 * - 로그인 시도 횟수 제한 (Brute Force 공격 방지)
 *
 * [사용 예시]
 * LoginRequest request = new LoginRequest("woody", "password123");
 * UserResponse user = authService.login(request);
 *
 * @param username 사용자명 - 로그인 ID
 * @param password 비밀번호
 */
public record LoginRequest(
        String username,
        String password
) {
}
