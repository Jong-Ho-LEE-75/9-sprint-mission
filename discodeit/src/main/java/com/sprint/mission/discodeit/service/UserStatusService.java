package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.request.UserStatusCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserStatusUpdateRequest;
import com.sprint.mission.discodeit.entity.UserStatus;
import java.util.List;
import java.util.UUID;

/**
 * ========================================
 * 사용자 상태 서비스 인터페이스
 * ========================================
 *
 * 이 인터페이스는 사용자 온라인 상태 관련 비즈니스 로직을 정의합니다.
 * 사용자가 온라인인지 오프라인인지 추적합니다.
 *
 * [온라인 판정 기준]
 * lastActiveAt이 현재 시간으로부터 5분 이내면 온라인으로 판단합니다.
 *
 * [구현체]
 * BasicUserStatusService: 기본 사용자 상태 서비스 구현
 */
public interface UserStatusService {
    /**
     * 사용자 상태를 생성합니다.
     *
     * [검증 사항]
     * - userId의 User가 존재하는지 확인
     * - 해당 User의 UserStatus가 이미 있는지 확인
     *
     * @param request 사용자 상태 생성 요청 (userId, lastAccessAt)
     * @return 생성된 UserStatus 엔티티
     * @throws java.util.NoSuchElementException User가 없을 경우
     * @throws IllegalArgumentException         이미 UserStatus가 존재할 경우
     */
    UserStatus create(UserStatusCreateRequest request);

    /**
     * ID로 사용자 상태를 조회합니다.
     *
     * @param id 사용자 상태 ID (UserStatus 자체의 ID)
     * @return 조회된 UserStatus 엔티티
     * @throws java.util.NoSuchElementException UserStatus가 없을 경우
     */
    UserStatus find(UUID id);

    /**
     * 사용자 ID로 상태를 조회합니다.
     *
     * User의 온라인 상태를 확인할 때 주로 사용합니다.
     *
     * @param userId 사용자 ID (User 엔티티의 ID)
     * @return 조회된 UserStatus 엔티티
     * @throws java.util.NoSuchElementException UserStatus가 없을 경우
     */
    UserStatus findByUserId(UUID userId);

    /**
     * 모든 사용자 상태를 조회합니다.
     *
     * @return 모든 UserStatus 목록
     */
    List<UserStatus> findAll();

    /**
     * ID로 사용자 상태를 업데이트합니다.
     *
     * @param id      사용자 상태 ID
     * @param request 수정 요청 (lastAccessAt)
     * @return 수정된 UserStatus 엔티티
     * @throws java.util.NoSuchElementException UserStatus가 없을 경우
     */
    UserStatus update(UUID id, UserStatusUpdateRequest request);

    /**
     * 사용자 ID로 상태를 업데이트합니다.
     *
     * 사용자 활동 시 마지막 접속 시간을 갱신할 때 주로 사용합니다.
     *
     * @param userId  사용자 ID
     * @param request 수정 요청 (lastAccessAt)
     * @return 수정된 UserStatus 엔티티
     * @throws java.util.NoSuchElementException UserStatus가 없을 경우
     */
    UserStatus updateByUserId(UUID userId, UserStatusUpdateRequest request);

    /**
     * 사용자 상태를 삭제합니다.
     *
     * @param id 삭제할 사용자 상태 ID
     */
    void delete(UUID id);
}
