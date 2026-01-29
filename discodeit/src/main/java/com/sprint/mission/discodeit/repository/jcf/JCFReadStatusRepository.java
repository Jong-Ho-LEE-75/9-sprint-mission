package com.sprint.mission.discodeit.repository.jcf;

import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * ========================================
 * Java Collection Framework 기반 읽기 상태 저장소 구현체
 * ========================================
 *
 * HashMap을 사용하여 읽기 상태 데이터를 메모리에 저장합니다.
 *
 * [특징]
 * 다양한 조회 메서드를 제공합니다:
 * - findByUserId: 특정 사용자의 모든 ReadStatus
 * - findByChannelId: 특정 채널의 모든 ReadStatus (= 참여자 목록)
 * - findByUserIdAndChannelId: 특정 사용자의 특정 채널 ReadStatus
 */
@Repository
@ConditionalOnProperty(name = "discodeit.repository.type", havingValue = "jcf", matchIfMissing = true)
public class JCFReadStatusRepository implements ReadStatusRepository {
    /**
     * 읽기 상태 데이터를 저장하는 HashMap
     * Key: UUID (ReadStatus ID), Value: ReadStatus
     */
    private final Map<UUID, ReadStatus> data = new HashMap<>();

    @Override
    public ReadStatus save(ReadStatus readStatus) {
        data.put(readStatus.getId(), readStatus);
        return readStatus;
    }

    @Override
    public Optional<ReadStatus> findById(UUID id) {
        return Optional.ofNullable(data.get(id));
    }

    @Override
    public List<ReadStatus> findAll() {
        return new ArrayList<>(data.values());
    }

    /**
     * 특정 사용자의 모든 읽기 상태를 조회합니다.
     *
     * 사용자가 참여 중인 채널 목록을 알아낼 때 사용합니다.
     */
    @Override
    public List<ReadStatus> findAllByUserId(UUID userId) {
        return data.values().stream()
                .filter(rs -> rs.getUserId().equals(userId))
                .toList();
    }

    /**
     * 특정 채널의 모든 읽기 상태를 조회합니다.
     *
     * PRIVATE 채널의 참여자 목록을 알아낼 때 사용합니다.
     */
    @Override
    public List<ReadStatus> findAllByChannelId(UUID channelId) {
        return data.values().stream()
                .filter(rs -> rs.getChannelId().equals(channelId))
                .toList();
    }

    /**
     * 사용자 ID와 채널 ID로 읽기 상태를 조회합니다.
     *
     * 두 조건을 AND로 결합하여 필터링합니다.
     * userId + channelId 조합은 유일하므로 결과는 0개 또는 1개입니다.
     */
    @Override
    public Optional<ReadStatus> findByUserIdAndChannelId(UUID userId, UUID channelId) {
        return data.values().stream()
                .filter(rs -> rs.getUserId().equals(userId) && rs.getChannelId().equals(channelId))
                .findFirst();
    }

    @Override
    public void deleteById(UUID id) {
        data.remove(id);
    }

    /**
     * 특정 채널의 모든 읽기 상태를 일괄 삭제합니다.
     *
     * 채널 삭제 시 관련 ReadStatus를 모두 삭제할 때 사용합니다.
     */
    @Override
    public void deleteAllByChannelId(UUID channelId) {
        data.values().removeIf(rs -> rs.getChannelId().equals(channelId));
    }

    @Override
    public boolean existsById(UUID id) {
        return data.containsKey(id);
    }
}
