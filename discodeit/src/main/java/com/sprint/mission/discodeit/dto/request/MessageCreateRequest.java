package com.sprint.mission.discodeit.dto.request;

import java.util.List;
import java.util.UUID;

/**
 * ========================================
 * 메시지 생성 요청 DTO
 * ========================================
 *
 * 이 record는 새 메시지 작성 시 클라이언트에서 서버로 전달되는 데이터를 담습니다.
 * 첨부파일은 별도의 파라미터(List<BinaryContentCreateRequest>)로 전달됩니다.
 *
 * [메시지 생성 과정]
 * 1. 클라이언트에서 메시지 내용, 채널 ID, 작성자 ID를 담은 요청 전송
 * 2. 첨부파일이 있으면 List<BinaryContentCreateRequest>도 함께 전송
 * 3. MessageService.create()에서 메시지와 첨부파일을 함께 저장
 *
 * [사용 예시]
 * // 텍스트만 있는 메시지
 * MessageCreateRequest request = new MessageCreateRequest(
 *     "안녕하세요!",
 *     channelId,
 *     userId
 * );
 * messageService.create(request, null);  // 첨부파일 없음
 *
 * // 첨부파일이 있는 메시지
 * MessageCreateRequest request = new MessageCreateRequest(
 *     "파일 첨부합니다.",
 *     channelId,
 *     userId
 * );
 * List<BinaryContentCreateRequest> attachments = List.of(fileRequest1, fileRequest2);
 * messageService.create(request, attachments);
 *
 * @param content   메시지 내용 (텍스트)
 * @param channelId 메시지를 보낼 채널 ID
 * @param authorId  메시지 작성자(나)의 ID
 */
public record MessageCreateRequest(
        String content,
        UUID channelId,
        UUID authorId
) {
}
