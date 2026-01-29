package com.sprint.mission.discodeit.entity;

import lombok.Getter;
import java.io.Serial;
import java.time.Instant;
import java.util.UUID;

/**
 * ========================================
 * 읽기 상태 엔티티 클래스
 * ========================================
 *
 * 이 클래스는 사용자가 특정 채널의 메시지를 마지막으로 읽은 시간을 추적합니다.
 * 카카오톡의 "읽음" 표시나 Discord의 "새 메시지" 표시와 유사한 기능입니다.
 *
 * [ReadStatus의 역할]
 * 1. 안 읽은 메시지 계산: 마지막으로 읽은 시간 이후의 메시지 수를 세어
 *    "안 읽은 메시지 10개" 같은 정보를 제공할 수 있습니다.
 *
 * 2. PRIVATE 채널 참여자 관리: PRIVATE 채널의 참여자는 ReadStatus로 관리됩니다.
 *    채널에 대한 ReadStatus가 있는 사용자만 그 채널에 접근할 수 있습니다.
 *
 * [어떻게 동작하나요?]
 * 1. 사용자가 채널에 입장하면 ReadStatus의 lastReadAt이 현재 시간으로 갱신됩니다.
 * 2. 새 메시지가 오면 메시지의 createdAt과 lastReadAt을 비교합니다.
 * 3. createdAt > lastReadAt인 메시지는 "안 읽은 메시지"입니다.
 *
 * [상속 관계]
 * ReadStatus → BaseEntity → Serializable
 *
 * [연관 관계]
 * - ReadStatus → User: 특정 사용자의 읽기 상태
 * - ReadStatus → Channel: 특정 채널에 대한 읽기 상태
 *
 * [어노테이션 설명]
 * @Getter (Lombok): 모든 필드에 대한 getter 메서드 자동 생성
 */
@Getter
public class ReadStatus extends BaseEntity {
    /**
     * 직렬화 버전 UID
     *
     * 파일 저장소(FileRepository) 사용 시 객체를 파일로 저장하기 위해 필요합니다.
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 사용자 ID
     *
     * 이 읽기 상태가 어떤 사용자의 것인지를 나타냅니다.
     * final로 선언되어 생성 후 변경 불가합니다.
     *
     * [왜 변경 불가능한가요?]
     * 읽기 상태는 특정 사용자와 특정 채널의 조합에 대해 하나만 존재해야 합니다.
     * 사용자를 변경하면 데이터 무결성이 깨질 수 있습니다.
     */
    private final UUID userId;

    /**
     * 채널 ID
     *
     * 이 읽기 상태가 어떤 채널에 대한 것인지를 나타냅니다.
     * final로 선언되어 생성 후 변경 불가합니다.
     */
    private final UUID channelId;

    /**
     * 마지막으로 메시지를 읽은 시간
     *
     * 사용자가 채널에서 마지막으로 메시지를 확인한 시간입니다.
     * 이 시간 이후에 작성된 메시지는 "읽지 않은 메시지"로 간주됩니다.
     *
     * [업데이트 시점]
     * - 사용자가 채널에 입장할 때
     * - 사용자가 채널에서 스크롤하여 메시지를 볼 때
     * - 앱/웹에서 채널이 화면에 표시될 때
     */
    private Instant lastReadAt;

    /**
     * ========================================
     * 읽기 상태 생성자
     * ========================================
     *
     * 새 읽기 상태를 생성할 때 사용합니다.
     * 주로 PRIVATE 채널 생성 시 각 참여자에 대해 자동으로 생성됩니다.
     *
     * [사용 예시]
     * // PRIVATE 채널 생성 시
     * ReadStatus status = new ReadStatus(
     *     userId,
     *     channelId,
     *     Instant.now()  // 현재 시간을 마지막 읽은 시간으로 설정
     * );
     *
     * @param userId     사용자 ID
     * @param channelId  채널 ID
     * @param lastReadAt 마지막으로 읽은 시간
     */
    public ReadStatus(UUID userId, UUID channelId, Instant lastReadAt) {
        // 부모 클래스(BaseEntity)의 생성자 호출
        // → id, createdAt, updatedAt이 자동으로 설정됨
        super();

        // 읽기 상태 정보 설정
        this.userId = userId;
        this.channelId = channelId;
        this.lastReadAt = lastReadAt;
    }

    /**
     * ========================================
     * 마지막 읽은 시간 업데이트 메서드
     * ========================================
     *
     * 사용자가 채널의 메시지를 읽었을 때 호출하여
     * 마지막 읽은 시간을 갱신합니다.
     *
     * [null 체크를 하는 이유]
     * API 요청에서 lastReadAt이 누락된 경우를 대비합니다.
     * null이 전달되면 아무 것도 변경하지 않습니다.
     *
     * @param lastReadAt 새로운 마지막 읽은 시간 (null이면 변경하지 않음)
     *
     * [사용 예시]
     * // 사용자가 채널에 입장했을 때
     * readStatus.update(Instant.now());
     */
    public void update(Instant lastReadAt) {
        if (lastReadAt != null) {
            this.lastReadAt = lastReadAt;
        }
        // 수정일시를 현재 시간으로 갱신
        updateTimeStamp();
    }
}
