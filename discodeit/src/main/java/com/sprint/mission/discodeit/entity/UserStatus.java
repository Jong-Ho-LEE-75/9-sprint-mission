package com.sprint.mission.discodeit.entity;

import lombok.Getter;
import java.io.Serial;
import java.time.Instant;
import java.util.UUID;

/**
 * ========================================
 * 사용자 상태 엔티티 클래스
 * ========================================
 *
 * 이 클래스는 사용자의 온라인/오프라인 상태를 추적합니다.
 * Discord나 카카오톡의 "접속 중" 표시와 유사한 기능입니다.
 *
 * [UserStatus의 역할]
 * 1. 온라인 상태 표시: 사용자 목록에서 누가 온라인인지 표시
 * 2. 마지막 접속 시간 표시: "30분 전 접속" 같은 정보 제공
 *
 * [온라인 판정 기준]
 * 마지막 활동 시간(lastActiveAt)이 현재 시간으로부터 5분 이내면 온라인으로 간주합니다.
 * 예: 지금이 10:00이면, 09:55 이후에 활동한 사용자는 온라인
 *
 * [User vs UserStatus - 왜 분리했나요?]
 * 1. 관심사의 분리: User는 계정 정보, UserStatus는 접속 정보
 * 2. 업데이트 빈도 차이: 상태는 자주 변경되지만 계정 정보는 거의 변경 안 됨
 * 3. 확장성: 나중에 "자리 비움", "다른 용무 중" 등 상태 추가 가능
 *
 * [상속 관계]
 * UserStatus → BaseEntity → Serializable
 *
 * [어노테이션 설명]
 * @Getter (Lombok): 모든 필드에 대한 getter 메서드 자동 생성
 */
@Getter
public class UserStatus extends BaseEntity {
    /**
     * 직렬화 버전 UID
     *
     * 파일 저장소 사용 시 객체를 파일로 저장하기 위해 필요합니다.
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 사용자 ID
     *
     * 이 상태가 어떤 사용자의 것인지를 나타냅니다.
     * final로 선언되어 생성 후 변경 불가합니다.
     *
     * [왜 User 객체가 아니라 UUID인가요?]
     * 느슨한 결합(Loose Coupling)을 유지하기 위해서입니다.
     * UserStatus를 조회할 때 User 전체 정보가 항상 필요한 것은 아닙니다.
     */
    private final UUID userId;

    /**
     * 마지막 활동 시간
     *
     * 사용자가 마지막으로 활동한 시간입니다.
     * 이 시간을 기준으로 온라인 여부를 판단합니다.
     *
     * [업데이트 시점]
     * - 로그인할 때
     * - 메시지를 보낼 때
     * - 채널을 전환할 때
     * - 앱이 포그라운드에 있을 때 주기적으로
     */
    private Instant lastActiveAt;

    /**
     * ========================================
     * 사용자 상태 생성자
     * ========================================
     *
     * 새 사용자 상태를 생성할 때 사용합니다.
     * 주로 회원가입 시 User와 함께 자동으로 생성됩니다.
     *
     * [사용 예시]
     * // 회원가입 시
     * UserStatus status = new UserStatus(
     *     user.getId(),
     *     Instant.now()  // 가입 시점을 마지막 활동으로 설정
     * );
     *
     * @param userId       사용자 ID
     * @param lastActiveAt 마지막 활동 시간
     */
    public UserStatus(UUID userId, Instant lastActiveAt) {
        // 부모 클래스(BaseEntity)의 생성자 호출
        // → id, createdAt, updatedAt이 자동으로 설정됨
        super();

        // 사용자 상태 정보 설정
        this.userId = userId;
        this.lastActiveAt = lastActiveAt;
    }

    /**
     * ========================================
     * 마지막 활동 시간 업데이트 메서드
     * ========================================
     *
     * 사용자가 활동할 때마다 호출하여 마지막 활동 시간을 갱신합니다.
     * 이를 통해 온라인 상태가 유지됩니다.
     *
     * @param lastActiveAt 새로운 마지막 활동 시간 (null이면 변경하지 않음)
     *
     * [사용 예시]
     * // 사용자가 메시지를 보냈을 때
     * userStatus.update(Instant.now());
     */
    public void update(Instant lastActiveAt) {
        if (lastActiveAt != null) {
            this.lastActiveAt = lastActiveAt;
        }
        // 수정일시를 현재 시간으로 갱신
        updateTimeStamp();
    }

    /**
     * ========================================
     * 온라인 상태 확인 메서드
     * ========================================
     *
     * 사용자가 현재 온라인 상태인지 확인합니다.
     * 마지막 활동 시간이 현재 시간으로부터 5분 이내이면 온라인으로 판단합니다.
     *
     * [판단 기준]
     * - 5분(300초) 이내 활동: 온라인 (true)
     * - 5분 초과 또는 lastActiveAt이 null: 오프라인 (false)
     *
     * [5분으로 설정한 이유]
     * - 너무 짧으면: 잠깐 자리를 비워도 오프라인으로 표시
     * - 너무 길면: 실제로 오프라인인데 온라인으로 표시
     * - 5분은 일반적으로 적절한 기준 (Discord도 비슷)
     *
     * @return true: 온라인, false: 오프라인
     *
     * [사용 예시]
     * if (userStatus.isOnline()) {
     *     System.out.println("사용자가 온라인입니다.");
     * } else {
     *     System.out.println("사용자가 오프라인입니다.");
     * }
     */
    public boolean isOnline() {
        // lastActiveAt이 null이면 활동 기록이 없으므로 오프라인
        if (lastActiveAt == null) {
            return false;
        }

        // 5분 전 시간 계산
        // Instant.now(): 현재 시간
        // minusSeconds(5 * 60): 300초(5분) 이전 시간
        Instant fiveMinutesAgo = Instant.now().minusSeconds(5 * 60);

        // lastActiveAt이 5분 전보다 이후인지 확인
        // isAfter: ~보다 이후인지 비교
        // 예: 10:00 > 09:55 → true (10:00이 09:55보다 이후)
        return lastActiveAt.isAfter(fiveMinutesAgo);
    }
}
