package com.sprint.mission.discodeit.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ========================================
 * 메시지 응답 DTO
 * ========================================
 *
 * 이 record는 메시지 정보를 클라이언트에게 반환할 때 사용됩니다.
 *
 * [Message Entity vs MessageResponse]
 * 현재는 구조가 동일하지만, 나중에 확장 시:
 * - 작성자 이름/프로필 정보 포함 가능
 * - 첨부파일 상세 정보 포함 가능
 * - 좋아요/반응 수 포함 가능
 *
 * [수정 여부 확인]
 * createdAt과 updatedAt을 비교하여 메시지가 수정되었는지 확인할 수 있습니다.
 * if (!message.createdAt().equals(message.updatedAt())) {
 *     // "(수정됨)" 표시
 * }
 *
 * [첨부파일 표시]
 * attachmentIds로 첨부파일 ID를 알 수 있습니다.
 * 실제 파일은 BinaryContentService.find(attachmentId)로 조회합니다.
 *
 * [사용 예시]
 * // 채널의 메시지 목록 조회
 * List<MessageResponse> messages = messageService.findAllByChannelId(channelId);
 * for (MessageResponse message : messages) {
 *     System.out.println(message.content());      // 메시지 내용
 *     System.out.println(message.authorId());     // 작성자 ID
 *     System.out.println(message.attachmentIds()); // 첨부파일 목록
 * }
 *
 * @param id            메시지 고유 식별자 (UUID)
 * @param content       메시지 내용 (텍스트)
 * @param channelId     메시지가 속한 채널 ID
 * @param authorId      메시지 작성자 ID
 * @param attachmentIds 첨부파일 ID 목록 (비어있을 수 있음)
 * @param createdAt     메시지 작성 시간
 * @param updatedAt     메시지 수정 시간 (수정 안 됐으면 createdAt과 동일)
 */
public record MessageResponse(
        UUID id,
        String content,
        UUID channelId,
        UUID authorId,
        List<UUID> attachmentIds,
        Instant createdAt,
        Instant updatedAt
) {
}
