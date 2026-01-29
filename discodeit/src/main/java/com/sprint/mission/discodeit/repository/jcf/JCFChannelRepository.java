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

    @Override
    public Channel save(Channel channel) {
        data.put(channel.getId(), channel);
        return channel;
    }

    @Override
    public Optional<Channel> findById(UUID id) {
        return Optional.ofNullable(data.get(id));
    }

    @Override
    public List<Channel> findAll() {
        return new ArrayList<>(data.values());
    }

    @Override
    public void deleteById(UUID id) {
        data.remove(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return data.containsKey(id);
    }
}
