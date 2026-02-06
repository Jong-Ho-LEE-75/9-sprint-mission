package com.sprint.mission.discodeit.repository.jcf;

import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * ========================================
 * Java Collection Framework 기반 채널 저장소 구현체
 * ========================================
 *
 * HashMap을 사용하여 채널 데이터를 메모리에 저장합니다.
 * JCFUserRepository와 동일한 패턴을 따릅니다.
 *
 * [어노테이션 설명]
 * @Repository: Spring Bean으로 등록
 * @ConditionalOnProperty: discodeit.repository.type=jcf 일 때만 활성화
 */
@Repository
@ConditionalOnProperty(name = "discodeit.repository.type", havingValue = "jcf", matchIfMissing = true)
public class JCFChannelRepository implements ChannelRepository {
    /**
     * 채널 데이터를 저장하는 HashMap
     * Key: UUID (채널 ID), Value: Channel (채널 객체)
     */
    private final Map<UUID, Channel> data = new HashMap<>();

    /**
     * 채널을 저장합니다.
     *
     * - 새 ID: 새로운 데이터 추가 (Create)
     * - 기존 ID: 기존 데이터 덮어쓰기 (Update)
     */
    @Override
    public Channel save(Channel channel) {
        data.put(channel.getId(), channel);
        return channel;
    }

    /**
     * ID로 채널을 조회합니다.
     *
     * 데이터가 없으면 Optional.empty() 반환.
     */
    @Override
    public Optional<Channel> findById(UUID id) {
        return Optional.ofNullable(data.get(id));
    }

    /**
     * 모든 채널을 조회합니다.
     *
     * 방어적 복사를 통해 새 리스트를 반환합니다.
     */
    @Override
    public List<Channel> findAll() {
        return new ArrayList<>(data.values());
    }

    /**
     * ID로 채널을 삭제합니다.
     */
    @Override
    public void deleteById(UUID id) {
        data.remove(id);
    }

    /**
     * ID로 채널 존재 여부를 확인합니다.
     */
    @Override
    public boolean existsById(UUID id) {
        return data.containsKey(id);
    }
}
