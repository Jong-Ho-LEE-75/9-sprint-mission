package com.sprint.mission.discodeit.repository.jcf;

import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * ========================================
 * Java Collection Framework 기반 사용자 상태 저장소 구현체
 * ========================================
 *
 * HashMap을 사용하여 사용자 상태 데이터를 메모리에 저장합니다.
 *
 * [특징]
 * userId로 조회/삭제하는 메서드가 있습니다.
 * (한 User당 하나의 UserStatus만 존재)
 */
@Repository
@ConditionalOnProperty(name = "discodeit.repository.type", havingValue = "jcf", matchIfMissing = true)
public class JCFUserStatusRepository implements UserStatusRepository {
    /**
     * 사용자 상태 데이터를 저장하는 HashMap
     * Key: UUID (UserStatus ID), Value: UserStatus
     */
    private final Map<UUID, UserStatus> data = new HashMap<>();

    @Override
    public UserStatus save(UserStatus userStatus) {
        data.put(userStatus.getId(), userStatus);
        return userStatus;
    }

    @Override
    public Optional<UserStatus> findById(UUID id) {
        return Optional.ofNullable(data.get(id));
    }

    /**
     * 사용자 ID로 상태를 조회합니다.
     *
     * User의 온라인 상태를 확인할 때 주로 사용합니다.
     */
    @Override
    public Optional<UserStatus> findByUserId(UUID userId) {
        return data.values().stream()
                .filter(us -> us.getUserId().equals(userId))
                .findFirst();
    }

    @Override
    public List<UserStatus> findAll() {
        return new ArrayList<>(data.values());
    }

    @Override
    public void deleteById(UUID id) {
        data.remove(id);
    }

    /**
     * 사용자 ID로 상태를 삭제합니다.
     *
     * User 삭제 시 연관된 UserStatus도 함께 삭제할 때 사용합니다.
     */
    @Override
    public void deleteByUserId(UUID userId) {
        data.values().removeIf(us -> us.getUserId().equals(userId));
    }

    @Override
    public boolean existsById(UUID id) {
        return data.containsKey(id);
    }

    /**
     * 사용자 ID로 존재 여부를 확인합니다.
     *
     * UserStatus 중복 생성 방지에 사용합니다.
     */
    @Override
    public boolean existsByUserId(UUID userId) {
        return data.values().stream()
                .anyMatch(us -> us.getUserId().equals(userId));
    }
}
