package com.sprint.mission.discodeit.entity;

import lombok.Getter;
import java.util.List;
import java.util.UUID;

/**
 * ========================================
 * 메시지 엔티티 클래스
 * ========================================
 *
 * 이 클래스는 채널 내에서 사용자가 작성한 메시지를 나타냅니다.
 * 카카오톡의 "메시지" 또는 Discord의 "채팅"과 같은 개념입니다.
 *
 * [메시지가 가지는 정보]
 * - id: 메시지 고유 식별자 (UUID) - BaseEntity에서 상속
 * - content: 메시지 내용 (텍스트)
 * - channelId: 메시지가 속한 채널의 ID
 * - authorId: 메시지 작성자의 ID
 * - attachmentIds: 첨부파일 ID 목록
 * - createdAt: 메시지 작성 시간 - BaseEntity에서 상속
 * - updatedAt: 마지막 수정 시간 - BaseEntity에서 상속
 *
 * [상속 관계]
 * Message → BaseEntity → Serializable
 *
 * [연관 관계]
 * - Message → Channel: 하나의 메시지는 하나의 채널에 속함
 * - Message → User: 하나의 메시지는 하나의 작성자가 있음
 * - Message → BinaryContent: 하나의 메시지는 여러 첨부파일을 가질 수 있음
 *
 * [어노테이션 설명]
 * @Getter (Lombok): 모든 필드에 대한 getter 메서드 자동 생성
 */
@Getter
public class Message extends BaseEntity {
    /**
     * 메시지 내용 (텍스트)
     *
     * 사용자가 작성한 실제 메시지 내용입니다.
     * 수정 가능합니다.
     *
     * 예: "안녕하세요!", "내일 회의는 몇 시인가요?", "파일 첨부했습니다."
     */
    private String content;

    /**
     * 메시지가 속한 채널 ID
     *
     * 이 메시지가 어느 채널에 작성되었는지를 나타냅니다.
     * final로 선언되어 메시지가 생성된 후에는 다른 채널로 이동할 수 없습니다.
     *
     * [왜 Channel 객체 대신 UUID를 저장하나요?]
     * 1. 느슨한 결합(Loose Coupling): 객체 간 직접 참조를 피해 독립성 유지
     * 2. 성능: 메시지 조회 시 항상 채널 정보를 함께 로드하지 않아도 됨
     * 3. 직렬화 용이: UUID는 단순 값이므로 직렬화가 쉬움
     */
    private final UUID channelId;

    /**
     * 메시지 작성자 ID
     *
     * 이 메시지를 작성한 사용자의 ID입니다.
     * final로 선언되어 작성자는 변경할 수 없습니다.
     *
     * 메시지 목록 표시 시 이 ID로 작성자 정보를 조회합니다.
     */
    private final UUID authorId;

    /**
     * 첨부파일 ID 목록
     *
     * 메시지에 첨부된 파일들의 ID 목록입니다.
     * 실제 파일 데이터는 BinaryContent 엔티티에 저장되고,
     * 여기에는 그 ID만 저장합니다.
     *
     * [List를 사용하는 이유]
     * 하나의 메시지에 여러 파일을 첨부할 수 있기 때문입니다.
     * 예: 이미지 3장 + PDF 1개를 함께 첨부
     *
     * final이지만 리스트 내용은 변경 가능합니다.
     * (List 자체를 다른 List로 교체하는 것은 불가,
     *  하지만 add(), remove()로 내용 변경은 가능)
     *
     * 비어있을 수 있음: 첨부파일이 없는 메시지
     */
    private final List<UUID> attachmentIds;

    /**
     * ========================================
     * 메시지 생성자
     * ========================================
     *
     * 새 메시지를 생성할 때 사용합니다.
     *
     * [사용 예시 - 첨부파일 없는 메시지]
     * Message message = new Message(
     *     "안녕하세요!",
     *     channelId,
     *     authorId,
     *     new ArrayList<>()  // 빈 리스트
     * );
     *
     * [사용 예시 - 첨부파일 있는 메시지]
     * List<UUID> attachments = List.of(fileId1, fileId2);
     * Message message = new Message(
     *     "파일 첨부합니다.",
     *     channelId,
     *     authorId,
     *     attachments
     * );
     *
     * @param content       메시지 내용
     * @param channelId     메시지가 속할 채널 ID
     * @param authorId      메시지 작성자 ID
     * @param attachmentIds 첨부파일 ID 목록 (없으면 빈 리스트)
     */
    public Message(String content, UUID channelId, UUID authorId, List<UUID> attachmentIds) {
        // 부모 클래스(BaseEntity)의 생성자 호출
        // → id, createdAt, updatedAt이 자동으로 설정됨
        super();

        // 메시지 정보 설정
        this.content = content;
        this.channelId = channelId;
        this.authorId = authorId;
        this.attachmentIds = attachmentIds;
    }

    /**
     * ========================================
     * 메시지 내용 업데이트 메서드
     * ========================================
     *
     * 메시지 내용을 수정할 때 사용합니다.
     * null이 아닌 값만 업데이트되며, 수정일시도 갱신됩니다.
     *
     * [왜 content만 수정 가능한가요?]
     * - channelId: 메시지를 다른 채널로 이동하는 것은 일반적이지 않음
     * - authorId: 작성자를 변경하는 것은 논리적으로 맞지 않음
     * - attachmentIds: 첨부파일 수정은 별도 API로 처리하는 것이 좋음
     *
     * [Discord/Slack 처럼 (수정됨) 표시]
     * updatedAt과 createdAt을 비교하면 메시지가 수정되었는지 알 수 있습니다.
     * if (!message.getUpdatedAt().equals(message.getCreatedAt())) {
     *     System.out.println("(수정됨)");
     * }
     *
     * @param content 새로운 메시지 내용 (null이면 변경하지 않음)
     *
     * [사용 예시]
     * message.update("수정된 메시지 내용입니다.");
     */
    public void update(String content) {
        // null이 아닌 경우에만 업데이트
        if (content != null) {
            this.content = content;
            // 수정일시를 현재 시간으로 갱신
            updateTimeStamp();
        }
    }
}
