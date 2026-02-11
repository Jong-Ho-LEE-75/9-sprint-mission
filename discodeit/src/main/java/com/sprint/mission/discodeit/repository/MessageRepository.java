package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.Message;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository {
    Message save(Message message);
    Optional<Message> findById(UUID id);
    List<Message> findAll();
    List<Message> findAllByChannelId(UUID channelId);  // 추가: 채널별 메시지 조회
    void deleteById(UUID id);
    void deleteAllByChannelId(UUID channelId);         // 추가: 채널 삭제 시 메시지 일괄 삭제
    boolean existsById(UUID id);
}
