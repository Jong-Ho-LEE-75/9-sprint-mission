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
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * MessageService 인터페이스의 기본 구현체.
 * 메시지의 생성, 조회, 수정, 삭제 및 첨부파일 관리 기능을 제공합니다.
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
     * 메시지를 생성합니다.
     * 첨부파일이 있는 경우 함께 저장합니다.
     *
     * @param request 메시지 생성 요청 정보
     * @param attachmentRequests 첨부파일 목록 (선택 사항)
     * @return 생성된 메시지 정보
     */
    @Override
    public MessageResponse create(MessageCreateRequest request, List<BinaryContentCreateRequest> attachmentRequests) {
        // 첨부파일 저장 (선택적)
        List<UUID> attachmentIds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(attachmentRequests)) {
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

        // Message 생성
        Message message = new Message(
                request.content(),
                request.channelId(),
                request.authorId(),
                attachmentIds
        );
        Message savedMessage = messageRepository.save(message);
        return toMessageResponse(savedMessage);
    }

    @Override
    public MessageResponse find(UUID id) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Message not found: " + id));
        return toMessageResponse(message);
    }

    @Override
    public List<MessageResponse> findAllByChannelId(UUID channelId) {
        return messageRepository.findAllByChannelId(channelId).stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Override
    public MessageResponse update(UUID id, MessageUpdateRequest request) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Message not found: " + id));
        message.update(request.content());
        Message savedMessage = messageRepository.save(message);
        return toMessageResponse(savedMessage);
    }

    /**
     * 메시지를 삭제합니다.
     * 연관된 첨부파일도 함께 삭제합니다.
     *
     * @param id 삭제할 메시지 ID
     * @throws NoSuchElementException 메시지를 찾을 수 없을 경우
     */
    @Override
    public void delete(UUID id) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Message not found: " + id));

        // 첨부파일 삭제
        if (!CollectionUtils.isEmpty(message.getAttachmentIds())) {
            for (UUID attachmentId : message.getAttachmentIds()) {
                binaryContentRepository.deleteById(attachmentId);
            }
        }

        // Message 삭제
        messageRepository.deleteById(id);
    }

    /**
     * Message 엔티티를 MessageResponse DTO로 변환합니다.
     *
     * @param message 변환할 Message 엔티티
     * @return MessageResponse DTO
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