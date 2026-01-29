package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.response.MessageResponse;
import java.util.List;
import java.util.UUID;

/**
 * ========================================
 * 메시지 서비스 인터페이스
 * ========================================
 *
 * 이 인터페이스는 메시지 관련 비즈니스 로직을 정의합니다.
 * 메시지 생성, 조회, 수정, 삭제 및 첨부파일 처리 기능을 제공합니다.
 *
 * [메시지의 구성]
 * - content: 텍스트 내용
 * - channelId: 소속 채널
 * - authorId: 작성자
 * - attachmentIds: 첨부파일 목록 (선택)
 *
 * [구현체]
 * BasicMessageService: 기본 메시지 서비스 구현
 */
public interface MessageService {
    /**
     * 메시지를 생성합니다.
     *
     * [처리 과정]
     * 1. 첨부파일이 있으면 BinaryContent로 저장
     * 2. Message 엔티티 생성 (첨부파일 ID 포함)
     * 3. Message 저장
     * 4. MessageResponse 반환
     *
     * @param request            메시지 생성 요청 (content, channelId, authorId)
     * @param attachmentRequests 첨부파일 목록 (선택, null 또는 빈 리스트 가능)
     * @return 생성된 메시지 정보
     */
    MessageResponse create(MessageCreateRequest request, List<BinaryContentCreateRequest> attachmentRequests);

    /**
     * ID로 메시지를 조회합니다.
     *
     * @param id 메시지 ID
     * @return 조회된 메시지 정보
     * @throws java.util.NoSuchElementException 메시지가 없을 경우
     */
    MessageResponse find(UUID id);

    /**
     * 특정 채널의 모든 메시지를 조회합니다.
     *
     * 채널 화면에서 메시지 목록을 표시할 때 사용합니다.
     *
     * @param channelId 채널 ID
     * @return 해당 채널의 메시지 목록
     */
    List<MessageResponse> findAllByChannelId(UUID channelId);

    /**
     * 메시지 내용을 수정합니다.
     *
     * [수정 가능 항목]
     * - content만 수정 가능
     * - channelId, authorId, attachmentIds는 수정 불가
     *
     * @param id      수정할 메시지 ID
     * @param request 수정 요청 (content)
     * @return 수정된 메시지 정보
     * @throws java.util.NoSuchElementException 메시지가 없을 경우
     */
    MessageResponse update(UUID id, MessageUpdateRequest request);

    /**
     * 메시지를 삭제합니다.
     *
     * [처리 과정 (Cascading Delete)]
     * 1. 메시지의 첨부파일 삭제
     * 2. 메시지 삭제
     *
     * @param id 삭제할 메시지 ID
     * @throws java.util.NoSuchElementException 메시지가 없을 경우
     */
    void delete(UUID id);
}
