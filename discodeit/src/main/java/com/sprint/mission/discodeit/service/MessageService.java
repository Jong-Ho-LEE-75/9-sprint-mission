package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.response.MessageResponse;
import java.util.List;
import java.util.UUID;

public interface MessageService {
    MessageResponse create(MessageCreateRequest request, List<BinaryContentCreateRequest> attachmentRequests);
    MessageResponse find(UUID id);
    List<MessageResponse> findAllByChannelId(UUID channelId);
    MessageResponse update(UUID id, MessageUpdateRequest request);
    void delete(UUID id);
}