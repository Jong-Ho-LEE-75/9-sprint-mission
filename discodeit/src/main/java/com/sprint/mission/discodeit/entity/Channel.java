package com.sprint.mission.discodeit.entity;
import lombok.Getter;

/**
 * ========================================
 * 채널 엔티티 클래스
 * ========================================
 *
 * 이 클래스는 메시지가 교환되는 채널을 나타냅니다.
 * Discord의 텍스트 채널이나 DM(Direct Message)과 유사한 개념입니다.
 *
 * [채널이란?]
 * 사용자들이 메시지를 주고받는 공간입니다.
 * 카카오톡의 "채팅방"과 비슷하다고 생각하면 됩니다.
 *
 * [채널의 두 가지 타입]
 * 1. PUBLIC 채널: 모든 사용자가 접근 가능한 공개 채널
 *    - 이름과 설명을 가짐
 *    - 수정 가능
 *    - 예: "공지사항", "일반 대화", "질문 게시판"
 *
 * 2. PRIVATE 채널: 초대된 사용자만 접근 가능한 비공개 채널
 *    - 이름과 설명이 없음 (null)
 *    - 수정 불가능
 *    - 참여자 목록은 ReadStatus를 통해 관리
 *    - 예: 1:1 대화, 비밀 그룹 채팅
 *
 * [상속 관계]
 * Channel → BaseEntity → Serializable
 *
 * Channel이 가지는 정보:
 * - id: 채널 고유 식별자 (UUID) - BaseEntity에서 상속
 * - type: 채널 타입 (PUBLIC/PRIVATE)
 * - name: 채널 이름 (PUBLIC만)
 * - description: 채널 설명 (PUBLIC만)
 * - createdAt: 채널 생성 시간 - BaseEntity에서 상속
 * - updatedAt: 마지막 수정 시간 - BaseEntity에서 상속
 *
 * [어노테이션 설명]
 * @Getter (Lombok): 모든 필드에 대한 getter 메서드 자동 생성
 */
@Getter
public class Channel extends BaseEntity {
    /**
     * 채널 타입 (PUBLIC 또는 PRIVATE)
     *
     * final 키워드: 한 번 설정되면 변경할 수 없음을 의미합니다.
     * 채널의 타입은 생성 후 변경할 수 없습니다.
     * (공개 채널을 비공개로 바꾸거나 그 반대는 불가)
     *
     * [final의 의미]
     * - 변수: 값을 한 번만 할당 가능
     * - 메서드: 오버라이드(재정의) 불가
     * - 클래스: 상속 불가
     */
    private final ChannelType type;

    /**
     * 채널 이름
     *
     * PUBLIC 채널의 경우에만 의미있는 값을 가집니다.
     * PRIVATE 채널의 경우 null입니다.
     *
     * 예: "공지사항", "일반 대화", "코드 리뷰"
     */
    private String name;

    /**
     * 채널 설명
     *
     * PUBLIC 채널의 경우에만 의미있는 값을 가집니다.
     * PRIVATE 채널의 경우 null입니다.
     *
     * 예: "중요 공지를 올리는 채널입니다", "자유롭게 대화하세요"
     */
    private String description;

    /**
     * ========================================
     * 채널 생성자
     * ========================================
     *
     * 새 채널을 생성할 때 사용합니다.
     *
     * [PUBLIC 채널 생성 예시]
     * Channel publicChannel = new Channel(
     *     ChannelType.PUBLIC,
     *     "공지사항",
     *     "중요 공지를 올리는 채널입니다"
     * );
     *
     * [PRIVATE 채널 생성 예시]
     * Channel privateChannel = new Channel(
     *     ChannelType.PRIVATE,
     *     null,  // PRIVATE 채널은 이름 없음
     *     null   // PRIVATE 채널은 설명 없음
     * );
     *
     * @param type        채널 타입 (PUBLIC 또는 PRIVATE)
     * @param name        채널 이름 (PRIVATE인 경우 null)
     * @param description 채널 설명 (PRIVATE인 경우 null)
     */
    public Channel(ChannelType type, String name, String description) {
        // 부모 클래스(BaseEntity)의 생성자 호출
        // → id, createdAt, updatedAt이 자동으로 설정됨
        super();

        // 채널 정보 설정
        this.type = type;
        this.name = name;
        this.description = description;
    }

    /**
     * ========================================
     * 채널 정보 업데이트 메서드
     * ========================================
     *
     * 채널의 이름과 설명을 수정할 때 사용합니다.
     * null이 아닌 값만 업데이트되며, 수정일시도 갱신됩니다.
     *
     * [주의사항]
     * PRIVATE 채널은 이 메서드를 호출해서는 안 됩니다!
     * BasicChannelService에서 PRIVATE 채널 수정 시도 시
     * IllegalArgumentException을 발생시킵니다.
     *
     * [null 체크를 하는 이유]
     * 부분 업데이트를 지원하기 위함입니다.
     * 예: 이름만 변경하고 싶을 때
     * channel.update("새 이름", null);  // 설명은 그대로
     *
     * @param name        새로운 채널 이름 (null이면 변경하지 않음)
     * @param description 새로운 채널 설명 (null이면 변경하지 않음)
     *
     * [사용 예시]
     * // 이름만 변경
     * channel.update("새로운 채널명", null);
     *
     * // 설명만 변경
     * channel.update(null, "새로운 설명입니다");
     *
     * // 둘 다 변경
     * channel.update("새 이름", "새 설명");
     */
    public void update(String name, String description) {
        // null이 아닌 경우에만 업데이트
        if (name != null) this.name = name;
        if (description != null) this.description = description;

        // 수정일시를 현재 시간으로 갱신
        updateTimeStamp();
    }
}
