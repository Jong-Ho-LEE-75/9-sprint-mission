package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.response.MessageResponse;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * ========================================
 * MessageService 인터페이스의 기본 구현체
 * ========================================
 *
 * 이 클래스는 메시지 관련 비즈니스 로직을 구현합니다.
 * 메시지 생성, 조회, 수정, 삭제 및 첨부파일 처리 기능을 제공합니다.
 *
 * [의존하는 Repository]
 * - MessageRepository: 메시지 데이터 저장/조회
 * - BinaryContentRepository: 첨부파일 저장/조회
 *
 * [어노테이션 설명]
 * @Service: Spring Bean으로 등록
 * @RequiredArgsConstructor: final 필드에 대한 생성자 자동 생성
 */
@Service
@RequiredArgsConstructor
public class BasicMessageService implements MessageService {
    /**
     * 메시지 정보를 저장하고 조회하는 리포지토리
     */
    private final MessageRepository messageRepository;

    /**
     * 메시지 첨부파일을 저장하고 조회하는 리포지토리
     */
    private final BinaryContentRepository binaryContentRepository;

    /**
     * ========================================
     * 메시지를 생성합니다.
     * ========================================
     *
     * [처리 순서]
     * 1. 첨부파일이 있으면 각각 BinaryContent로 저장
     * 2. 첨부파일 ID 목록 수집
     * 3. Message 엔티티 생성 (첨부파일 ID 포함)
     * 4. Message 저장
     * 5. MessageResponse 반환
     */
    @Override
    public MessageResponse create(MessageCreateRequest request, List<BinaryContentCreateRequest> attachmentRequests) {
        // 1단계: 첨부파일 저장 (선택적)
        List<UUID> attachmentIds = new ArrayList<>();

        if (attachmentRequests != null && !attachmentRequests.isEmpty()) {
            // 각 첨부파일을 BinaryContent로 저장하고 ID 수집
            for (BinaryContentCreateRequest attachmentRequest : attachmentRequests) {
                BinaryContent attachment = new BinaryContent(
                        attachmentRequest.fileName(),
                        attachmentRequest.contentType(),
                        attachmentRequest.data()
                );
                BinaryContent savedAttachment = binaryContentRepository.save(attachment);
                attachmentIds.add(savedAttachment.getId());
            }
        }

        // 2단계: Message 엔티티 생성
        Message message = new Message(
                request.content(),
                request.channelId(),
                request.authorId(),
                attachmentIds  // 첨부파일이 없으면 빈 리스트
        );

        // 3단계: 저장 및 반환
        Message savedMessage = messageRepository.save(message);
        return toMessageResponse(savedMessage);
    }

    /**
     * ID로 메시지를 조회합니다.
     *
     * ※ Optional/orElseThrow 상세 설명은 BasicBinaryContentService.find() 참조
     *
     * @param id 조회할 메시지 ID
     * @return 조회된 MessageResponse DTO
     * @throws NoSuchElementException 해당 ID의 메시지가 없을 경우
     */
    @Override
    public MessageResponse find(UUID id) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Message not found: " + id));
        return toMessageResponse(message);
    }

    /**
     * 특정 채널의 모든 메시지를 조회합니다.
     *
     * 채널 화면에서 메시지 목록을 표시할 때 사용합니다.
     *
     * [Stream 데이터 흐름]
     * List<Message> → stream() → map(DTO변환) → toList() → List<MessageResponse>
     *
     * ※ Stream API 상세 설명은 BasicChannelService.findAllByUserId() 참조
     *
     * @param channelId 조회할 채널 ID
     * @return 해당 채널의 MessageResponse 목록
     */
    @Override
    public List<MessageResponse> findAllByChannelId(UUID channelId) {
        return messageRepository.findAllByChannelId(channelId).stream()
                .map(this::toMessageResponse)
                .toList();
    }

    /**
     * 메시지 내용을 수정합니다.
     *
     * 수정 가능한 것은 content(본문)뿐입니다.
     * 작성자, 채널, 첨부파일은 수정할 수 없습니다.
     *
     * [조회 → 수정 → 저장 패턴]
     * 표준적인 update 패턴입니다.
     *
     * @param id 수정할 메시지 ID
     * @param request 수정 요청 (content)
     * @return 수정된 MessageResponse DTO
     * @throws NoSuchElementException 해당 ID의 메시지가 없을 경우
     */
    @Override
    public MessageResponse update(UUID id, MessageUpdateRequest request) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Message not found: " + id));

        message.update(request.content());
        Message savedMessage = messageRepository.save(message);
        return toMessageResponse(savedMessage);
    }

    /**
     * ========================================
     * 메시지를 삭제합니다. (Cascading Delete)
     * ========================================
     *
     * [삭제 순서]
     * 1. 메시지 조회
     * 2. 첨부파일 삭제 (있는 경우)
     * 3. 메시지 삭제
     */
    @Override
    public void delete(UUID id) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Message not found: " + id));

        // 첨부파일 삭제
        if (message.getAttachmentIds() != null) {
            for (UUID attachmentId : message.getAttachmentIds()) {
                binaryContentRepository.deleteById(attachmentId);
            }
        }

        // 메시지 삭제
        messageRepository.deleteById(id);
    }

    /**
     * Message 엔티티를 MessageResponse DTO로 변환
     */
    private MessageResponse toMessageResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getContent(),
                message.getChannelId(),
                message.getAuthorId(),
                message.getAttachmentIds(),
                message.getCreatedAt(),
                message.getUpdatedAt()
        );
    }
}
