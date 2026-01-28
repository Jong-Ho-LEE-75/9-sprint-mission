package com.sprint.mission.discodeit.service.jcf;

import com.sprint.mission.discodeit.service.ChannelService;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.entity.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JCFMessageService implements MessageService {

    // 1. [의존성] 다른 서비스
    private final UserService userService;
    private final ChannelService channelService;

    // List 저장소 사용 (ID 검색 시 순회 필요)
    private final List<Message> messages = new ArrayList<>();

    // 2. [의존성 주입]
    public JCFMessageService(UserService userService, ChannelService channelService) {
        this.userService = userService;
        this.channelService = channelService;
    }

    @Override
    public Message create(String content, UUID channelId, UUID userId) {
        // ==========================================
        // 3. [검증] 도메인 모델 간 관계 확인
        // ==========================================

        // (1) 회원 존재 여부 검증 (UUID 사용)
        if (userService.find(userId) == null) {
            throw new IllegalArgumentException("존재하지 않는 회원(ID:" + userId + ")입니다.");
        }

        // (2) 채널 존재 여부 검증 (UUID 사용)
        if (channelService.find(channelId) == null) {
            throw new IllegalArgumentException("존재하지 않는 채널(ID:" + channelId + ")입니다.");
        }

        Message message = new Message(content, channelId, userId);

        messages.add(message);
        return message;
    }

    @Override
    public Message find(UUID id) {
        for (Message m : messages) {
            if (m.getId().equals(id)) {
                return m;
            }
        }
        return null;
    }

    @Override
    public List<Message> findAll() {
        return messages;
    }

    @Override
    public Message update(UUID id, String content) {
        Message message = find(id);
        if (message != null) {
            message.update(content);
        }
        return message;
    }

    @Override
    public void delete(UUID id) {
        Message message = find(id);
        if (message != null) {
            messages.remove(message);
        }
    }
}