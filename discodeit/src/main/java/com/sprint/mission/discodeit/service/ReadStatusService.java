package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.request.ReadStatusCreateRequest;
import com.sprint.mission.discodeit.dto.request.ReadStatusUpdateRequest;
import com.sprint.mission.discodeit.entity.ReadStatus;
import java.util.List;
import java.util.UUID;

/**
 * ========================================
 * 읽기 상태 서비스 인터페이스
 * ========================================
 *
 * 이 인터페이스는 읽기 상태 관련 비즈니스 로직을 정의합니다.
 * 사용자가 채널의 메시지를 어디까지 읽었는지 추적합니다.
 *
 * [용도]
 * 1. 안 읽은 메시지 수 계산
 * 2. PRIVATE 채널 참여자 관리
 *
 * [구현체]
 * BasicReadStatusService: 기본 읽기 상태 서비스 구현
 */
public interface ReadStatusService {
    /**
     * 읽기 상태를 생성합니다.
     *
     * [검증 사항]
     * - userId의 User가 존재하는지 확인
     * - channelId의 Channel이 존재하는지 확인
     * - 같은 userId + channelId 조합의 ReadStatus가 이미 있는지 확인
     *
     * @param request 읽기 상태 생성 요청 (userId, channelId, lastReadAt)
     * @return 생성된 ReadStatus 엔티티
     * @throws java.util.NoSuchElementException User 또는 Channel이 없을 경우
     * @throws IllegalArgumentException         이미 ReadStatus가 존재할 경우
     */
    ReadStatus create(ReadStatusCreateRequest request);

    /**
     * ID로 읽기 상태를 조회합니다.
     *
     * @param id 읽기 상태 ID
     * @return 조회된 ReadStatus 엔티티
     * @throws java.util.NoSuchElementException ReadStatus가 없을 경우
     */
    ReadStatus find(UUID id);

    /**
     * 특정 사용자의 모든 읽기 상태를 조회합니다.
     *
     * 사용자가 참여 중인 채널 목록을 알아낼 때 사용합니다.
     *
     * @param userId 사용자 ID
     * @return 해당 사용자의 읽기 상태 목록
     */
    List<ReadStatus> findAllByUserId(UUID userId);

    /**
     * 읽기 상태를 업데이트합니다.
     *
     * 사용자가 채널에 입장하거나 메시지를 읽을 때 호출합니다.
     *
     * @param id      읽기 상태 ID
     * @param request 수정 요청 (lastReadAt)
     * @return 수정된 ReadStatus 엔티티
     * @throws java.util.NoSuchElementException ReadStatus가 없을 경우
     */
    ReadStatus update(UUID id, ReadStatusUpdateRequest request);

    /**
     * 읽기 상태를 삭제합니다.
     *
     * @param id 삭제할 읽기 상태 ID
     */
    void delete(UUID id);
}
