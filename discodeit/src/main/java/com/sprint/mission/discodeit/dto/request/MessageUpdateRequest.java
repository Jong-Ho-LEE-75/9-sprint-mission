package com.sprint.mission.discodeit.dto.request;

/**
 * ========================================
 * 메시지 수정 요청 DTO
 * ========================================
 *
 * 이 record는 메시지 내용 수정 시 클라이언트에서 서버로 전달되는 데이터를 담습니다.
 *
 * [수정 가능한 항목]
 * 메시지에서 수정할 수 있는 것은 내용(content)뿐입니다.
 * - 채널 ID: 변경 불가 (메시지를 다른 채널로 이동하지 않음)
 * - 작성자 ID: 변경 불가 (작성자는 바뀌지 않음)
 * - 첨부파일: 별도 API로 처리 (이 DTO에서는 다루지 않음)
 *
 * [수정 표시]
 * 메시지가 수정되면 updatedAt이 갱신됩니다.
 * updatedAt != createdAt 이면 "(수정됨)" 표시를 할 수 있습니다.
 *
 * [사용 예시]
 * MessageUpdateRequest request = new MessageUpdateRequest("수정된 메시지입니다.");
 * messageService.update(messageId, request);
 *
 * @param content 변경할 메시지 내용
 */
public record MessageUpdateRequest(
        String content
) {
}
