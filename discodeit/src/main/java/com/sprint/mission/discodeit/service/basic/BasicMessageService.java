package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
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
    public Message create(MessageCreateRequest request, List<BinaryContentCreateRequest> attachmentRequests) {
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
        return messageRepository.save(message);
    }

    @Override
    public Message find(UUID id) {
        return messageRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Message not found: " + id));
    }

    @Override
    public List<Message> findAllByChannelId(UUID channelId) {
        return messageRepository.findAllByChannelId(channelId);
    }

    @Override
    public Message update(UUID id, MessageUpdateRequest request) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Message not found: " + id));
        message.update(request.getContent());
        return messageRepository.save(message);
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
}