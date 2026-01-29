package com.sprint.mission.discodeit.repository.jcf;

import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.repository.MessageRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * ========================================
 * Java Collection Framework 기반 메시지 저장소 구현체
 * ========================================
 *
 * HashMap을 사용하여 메시지 데이터를 메모리에 저장합니다.
 *
 * [특징]
 * - findAllByChannelId: 특정 채널의 메시지만 필터링하여 반환
 * - deleteAllByChannelId: 채널 삭제 시 관련 메시지 일괄 삭제
 */
@Repository
@ConditionalOnProperty(name = "discodeit.repository.type", havingValue = "jcf", matchIfMissing = true)
public class JCFMessageRepository implements MessageRepository {
    /**
     * 메시지 데이터를 저장하는 HashMap
     * Key: UUID (메시지 ID), Value: Message (메시지 객체)
     */
    private final Map<UUID, Message> data = new HashMap<>();

    @Override
    public Message save(Message message) {
        data.put(message.getId(), message);
        return message;
    }

    @Override
    public Optional<Message> findById(UUID id) {
        return Optional.ofNullable(data.get(id));
    }

    @Override
    public List<Message> findAll() {
        return new ArrayList<>(data.values());
    }

    /**
     * 특정 채널의 모든 메시지를 조회합니다.
     *
     * Stream의 filter를 사용하여 channelId가 일치하는 메시지만 필터링합니다.
     *
     * [Stream 체인 설명]
     * 1. data.values().stream(): 모든 메시지를 Stream으로
     * 2. filter(): channelId가 일치하는 것만 통과
     * 3. toList(): 결과를 List로 변환 (Java 16+)
     */
    @Override
    public List<Message> findAllByChannelId(UUID channelId) {
        return data.values().stream()
                .filter(msg -> msg.getChannelId().equals(channelId))
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        data.remove(id);
    }

    /**
     * 특정 채널의 모든 메시지를 일괄 삭제합니다.
     *
     * Collection.removeIf()를 사용하여 조건에 맞는 요소를 제거합니다.
     * 이 메서드는 원본 Collection을 직접 수정합니다.
     *
     * [removeIf vs filter]
     * - removeIf: 원본 수정 (이 경우 data에서 직접 삭제)
     * - filter: 새로운 Stream 생성 (원본 유지)
     */
    @Override
    public void deleteAllByChannelId(UUID channelId) {
        // removeIf(조건): 조건이 true인 요소를 모두 삭제
        data.values().removeIf(msg -> msg.getChannelId().equals(channelId));
    }

    @Override
    public boolean existsById(UUID id) {
        return data.containsKey(id);
    }
}
