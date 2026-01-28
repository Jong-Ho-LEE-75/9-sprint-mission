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

@Service
@RequiredArgsConstructor
public class BasicMessageService implements MessageService {
    private final MessageRepository messageRepository;
    private final BinaryContentRepository binaryContentRepository;

    @Override
    public MessageResponse create(MessageCreateRequest request, List<BinaryContentCreateRequest> attachmentRequests) {
        // 첨부파일 저장 (선택적)
        List<UUID> attachmentIds = new ArrayList<>();
        if (attachmentRequests != null && !attachmentRequests.isEmpty()) {
            for (BinaryContentCreateRequest attachmentRequest : attachmentRequests) {
                BinaryContent attachment = new BinaryContent(
                        attachmentRequest.getFileName(),
                        attachmentRequest.getContentType(),
                        attachmentRequest.getData()
                );
                BinaryContent savedAttachment = binaryContentRepository.save(attachment);
                attachmentIds.add(savedAttachment.getId());
            }
        }

        // Message 생성
        Message message = new Message(
                request.getContent(),
                request.getChannelId(),
                request.getAuthorId(),
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
        message.update(request.getContent());
        Message savedMessage = messageRepository.save(message);
        return toMessageResponse(savedMessage);
    }

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

        // Message 삭제
        messageRepository.deleteById(id);
    }

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