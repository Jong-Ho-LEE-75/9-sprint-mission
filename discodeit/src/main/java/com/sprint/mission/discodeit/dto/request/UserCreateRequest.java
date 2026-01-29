package com.sprint.mission.discodeit.dto.request;

/**
 * ========================================
 * 사용자 생성(회원가입) 요청 DTO
 * ========================================
 *
 * 이 record는 회원가입 시 클라이언트에서 서버로 전달되는 데이터를 담습니다.
 *
 * [DTO(Data Transfer Object)란?]
 * 계층 간 데이터 전달을 위한 객체입니다.
 * Controller ↔ Service ↔ Repository 사이에서 데이터를 주고받을 때 사용합니다.
 *
 * [왜 Entity를 직접 사용하지 않고 DTO를 사용하나요?]
 * 1. 보안: Entity에는 password 같은 민감한 필드가 있는데,
 *    DTO를 사용하면 필요한 필드만 노출할 수 있습니다.
 * 2. 유연성: API 요청/응답 형식이 Entity 구조와 다를 수 있습니다.
 * 3. 검증: DTO에서 입력값 검증을 수행할 수 있습니다.
 *
 * [record란? (Java 16+)]
 * 불변(immutable) 데이터 클래스를 간결하게 정의하는 방법입니다.
 * record를 사용하면 자동으로 생성되는 것들:
 * - private final 필드
 * - 생성자
 * - getter 메서드 (필드명과 동일: username(), email(), password())
 * - equals(), hashCode(), toString()
 *
 * [record vs class]
 * // 기존 class 방식
 * public class UserCreateRequest {
 *     private final String username;
 *     private final String email;
 *     private final String password;
 *
 *     public UserCreateRequest(String username, String email, String password) {
 *         this.username = username;
 *         this.email = email;
 *         this.password = password;
 *     }
 *
 *     public String username() { return username; }
 *     public String email() { return email; }
 *     public String password() { return password; }
 *     // equals, hashCode, toString...
 * }
 *
 * // record 방식 - 위의 모든 코드가 한 줄로!
 * public record UserCreateRequest(String username, String email, String password) {}
 *
 * [사용 예시]
 * UserCreateRequest request = new UserCreateRequest("woody", "woody@codeit.com", "password123");
 * String name = request.username();  // "woody"
 *
 * @param username 사용자명 - 로그인 ID로 사용됨, 시스템 내 고유해야 함
 * @param email    이메일 주소 - 고유해야 함
 * @param password 비밀번호
 */
public record UserCreateRequest(
        String username,
        String email,
        String password
) {
}
