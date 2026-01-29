package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.ReadStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ========================================
 * 읽기 상태 저장소 인터페이스
 * ========================================
 *
 * 이 인터페이스는 ReadStatus 엔티티의 영속성을 담당합니다.
 * 사용자가 채널의 메시지를 어디까지 읽었는지 추적합니다.
 *
 * [구현체]
 * - JCFReadStatusRepository: HashMap을 사용하여 메모리에 저장
 * - FileReadStatusRepository: 파일 시스템에 직렬화하여 저장
 *
 * [ReadStatus의 역할]
 * 1. 안 읽은 메시지 수 계산
 * 2. PRIVATE 채널의 참여자 관리
 *
 * [특징]
 * userId + channelId 조합은 유일해야 합니다.
 * (한 사용자는 한 채널에 대해 하나의 ReadStatus만 가짐)
 */
public interface ReadStatusRepository {
    /**
     * 읽기 상태를 저장합니다.
     *
     * Create와 Update 모두에 사용됩니다.
     *
     * @param readStatus 저장할 읽기 상태
     * @return 저장된 읽기 상태
     */
    ReadStatus save(ReadStatus readStatus);

    /**
     * ID로 읽기 상태를 조회합니다.
     *
     * @param id 읽기 상태 ID
     * @return 조회된 읽기 상태 (없으면 Optional.empty())
     */
    Optional<ReadStatus> findById(UUID id);

    /**
     * 모든 읽기 상태를 조회합니다.
     *
     * @return 모든 읽기 상태 목록
     */
    List<ReadStatus> findAll();

    /**
     * 특정 사용자의 모든 읽기 상태를 조회합니다.
     *
     * 사용자가 참여 중인 채널(특히 PRIVATE 채널) 목록을 알아낼 때 사용합니다.
     *
     * [사용 예시]
     * List<ReadStatus> myReadStatuses = readStatusRepository.findAllByUserId(userId);
     * // 각 ReadStatus의 channelId로 참여 중인 PRIVATE 채널을 알 수 있음
     *
     * @param userId 사용자 ID
     * @return 해당 사용자의 읽기 상태 목록
     */
    List<ReadStatus> findAllByUserId(UUID userId);

    /**
     * 특정 채널의 모든 읽기 상태를 조회합니다.
     *
     * PRIVATE 채널의 참여자 목록을 알아낼 때 사용합니다.
     *
     * [사용 예시]
     * List<ReadStatus> channelReadStatuses = readStatusRepository.findAllByChannelId(channelId);
     * List<UUID> participantIds = channelReadStatuses.stream()
     *     .map(ReadStatus::getUserId)
     *     .toList();
     *
     * @param channelId 채널 ID
     * @return 해당 채널의 읽기 상태 목록 (= 참여자 목록)
     */
    List<ReadStatus> findAllByChannelId(UUID channelId);

    /**
     * 사용자 ID와 채널 ID로 읽기 상태를 조회합니다.
     *
     * 특정 사용자의 특정 채널에 대한 읽기 상태를 조회합니다.
     * PRIVATE 채널 접근 권한 확인에도 사용됩니다.
     * (ReadStatus가 있으면 참여자, 없으면 비참여자)
     *
     * [사용 예시 - 접근 권한 확인]
     * Optional<ReadStatus> status = readStatusRepository.findByUserIdAndChannelId(userId, channelId);
     * if (status.isPresent()) {
     *     // 이 사용자는 이 PRIVATE 채널에 접근 가능
     * }
     *
     * @param userId    사용자 ID
     * @param channelId 채널 ID
     * @return 조회된 읽기 상태 (없으면 Optional.empty())
     */
    Optional<ReadStatus> findByUserIdAndChannelId(UUID userId, UUID channelId);

    /**
     * ID로 읽기 상태를 삭제합니다.
     *
     * @param id 삭제할 읽기 상태 ID
     */
    void deleteById(UUID id);

    /**
     * 특정 채널의 모든 읽기 상태를 일괄 삭제합니다.
     *
     * 채널 삭제 시 해당 채널의 모든 ReadStatus를 삭제할 때 사용합니다.
     *
     * @param channelId 채널 ID
     */
    void deleteAllByChannelId(UUID channelId);

    /**
     * ID로 읽기 상태의 존재 여부를 확인합니다.
     *
     * @param id 읽기 상태 ID
     * @return true: 존재함, false: 존재하지 않음
     */
    boolean existsById(UUID id);
}
