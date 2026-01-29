package com.sprint.mission.discodeit.entity;

import lombok.Getter;
import java.util.UUID;

/**
 * ========================================
 * 사용자 엔티티 클래스
 * ========================================
 *
 * 이 클래스는 시스템에 등록된 사용자 정보를 나타냅니다.
 * BaseEntity를 상속받아 id, createdAt, updatedAt을 자동으로 가집니다.
 *
 * [Entity(엔티티)란?]
 * 데이터베이스 테이블에 대응하는 Java 클래스입니다.
 * 테이블의 각 행(row)이 Entity의 인스턴스(객체)가 됩니다.
 * 예: users 테이블 ↔ User 클래스
 *
 * [User가 가지는 정보]
 * - id: 사용자 고유 식별자 (UUID) - BaseEntity에서 상속
 * - username: 로그인에 사용되는 사용자명
 * - email: 이메일 주소 (중복 불가)
 * - password: 비밀번호
 * - profileId: 프로필 이미지의 ID (BinaryContent 참조)
 * - createdAt: 계정 생성 시간 - BaseEntity에서 상속
 * - updatedAt: 마지막 정보 수정 시간 - BaseEntity에서 상속
 *
 * [상속 관계]
 * User → BaseEntity → Serializable (인터페이스)
 *
 * [어노테이션 설명]
 * @Getter (Lombok): 모든 필드에 대한 getter 메서드 자동 생성
 *   - getUsername(), getEmail(), getPassword(), getProfileId()
 *   - 상속받은 getId(), getCreatedAt(), getUpdatedAt()도 사용 가능
 */
@Getter
public class User extends BaseEntity {
    /**
     * 사용자명 (Username)
     *
     * 로그인 시 사용되는 고유한 이름입니다.
     * 시스템 내에서 중복될 수 없습니다.
     *
     * [제약 조건]
     * - 중복 불가 (UserRepository.existsByUsername()으로 확인)
     * - null 불가
     *
     * 예: "woody", "alice", "bob"
     */
    private String username;

    /**
     * 이메일 주소 (Email)
     *
     * 사용자의 이메일 주소입니다.
     * 비밀번호 찾기, 알림 전송 등에 사용됩니다.
     *
     * [제약 조건]
     * - 중복 불가 (UserRepository.existsByEmail()로 확인)
     * - null 불가
     * - 유효한 이메일 형식이어야 함 (예: user@example.com)
     *
     * 예: "woody@codeit.com"
     */
    private String email;

    /**
     * 비밀번호 (Password)
     *
     * 사용자 인증에 사용되는 비밀번호입니다.
     *
     * [보안 주의사항]
     * 현재는 평문으로 저장되어 있지만, 실제 프로덕션에서는
     * BCrypt 등의 해시 알고리즘으로 암호화해야 합니다!
     * 평문 비밀번호를 저장하면 보안 취약점이 됩니다.
     *
     * [해시란?]
     * 원본 데이터를 고정 길이의 다른 값으로 변환하는 것입니다.
     * 해시된 값으로는 원본을 알아낼 수 없어 보안에 좋습니다.
     * 예: "password123" → "$2a$10$N9qo8uLOickgx2ZMRZo..."
     */
    private String password;

    /**
     * 프로필 이미지 ID
     *
     * 사용자의 프로필 이미지를 참조하는 ID입니다.
     * 실제 이미지는 BinaryContent 엔티티에 저장되고,
     * 여기에는 그 ID만 저장합니다.
     *
     * [왜 이미지 데이터를 직접 저장하지 않나요?]
     * 1. 이미지는 크기가 크기 때문에 별도로 관리하는 것이 효율적
     * 2. 같은 이미지를 여러 곳에서 재사용 가능
     * 3. 사용자 정보 조회 시 이미지까지 항상 불러오면 느려짐
     *
     * null일 수 있음: 프로필 이미지가 없는 사용자
     */
    private UUID profileId;

    /**
     * ========================================
     * 사용자 생성자
     * ========================================
     *
     * 새 사용자를 생성할 때 사용합니다.
     * super()를 호출하여 BaseEntity의 id, createdAt, updatedAt이 자동 설정됩니다.
     *
     * [생성자 호출 순서]
     * 1. super() 호출 → BaseEntity 생성자 실행 (id, createdAt, updatedAt 설정)
     * 2. 이 생성자의 코드 실행 (username, email, password, profileId 설정)
     *
     * @param username  사용자명 (고유해야 함)
     * @param email     이메일 주소 (고유해야 함)
     * @param password  비밀번호
     * @param profileId 프로필 이미지 ID (없으면 null)
     *
     * [사용 예시]
     * User user = new User("woody", "woody@codeit.com", "password123", null);
     * User userWithProfile = new User("alice", "alice@codeit.com", "pass", profileUUID);
     */
    public User(String username, String email, String password, UUID profileId) {
        // 부모 클래스(BaseEntity)의 생성자 호출
        // → id, createdAt, updatedAt이 자동으로 설정됨
        super();

        // 사용자 고유 정보 설정
        this.username = username;
        this.email = email;
        this.password = password;
        this.profileId = profileId;
    }

    /**
     * ========================================
     * 사용자 정보 업데이트 메서드
     * ========================================
     *
     * 사용자 정보를 수정할 때 사용합니다.
     * null이 아닌 값만 업데이트되며, 수정일시(updatedAt)도 자동으로 갱신됩니다.
     *
     * [null 체크를 하는 이유]
     * 부분 업데이트를 지원하기 위함입니다.
     * 예: 이메일만 변경하고 싶을 때
     * user.update(null, "new@email.com", null, null);
     * → username, password, profileId는 변경되지 않음
     *
     * @param username  새로운 사용자명 (null이면 변경하지 않음)
     * @param email     새로운 이메일 (null이면 변경하지 않음)
     * @param password  새로운 비밀번호 (null이면 변경하지 않음)
     * @param profileId 새로운 프로필 이미지 ID (null이면 변경하지 않음)
     *
     * [사용 예시]
     * // 이메일만 변경
     * user.update(null, "new@email.com", null, null);
     *
     * // 비밀번호만 변경
     * user.update(null, null, "newPassword123", null);
     *
     * // 모든 정보 변경
     * user.update("newUsername", "new@email.com", "newPass", newProfileId);
     */
    public void update(String username, String email, String password, UUID profileId) {
        // null이 아닌 경우에만 업데이트
        if (username != null) this.username = username;
        if (email != null) this.email = email;
        if (password != null) this.password = password;
        if (profileId != null) this.profileId = profileId;

        // BaseEntity의 메서드 호출 - 수정일시를 현재 시간으로 갱신
        updateTimeStamp();
    }
}
