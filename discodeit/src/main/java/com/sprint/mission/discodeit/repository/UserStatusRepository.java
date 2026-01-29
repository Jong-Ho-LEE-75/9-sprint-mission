package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.UserStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ========================================
 * 사용자 상태 저장소 인터페이스
 * ========================================
 *
 * 이 인터페이스는 UserStatus 엔티티의 영속성을 담당합니다.
 * 사용자의 온라인/오프라인 상태와 마지막 활동 시간을 저장합니다.
 *
 * [구현체]
 * - JCFUserStatusRepository: HashMap을 사용하여 메모리에 저장
 * - FileUserStatusRepository: 파일 시스템에 직렬화하여 저장
 *
 * [특징]
 * 한 User당 하나의 UserStatus만 존재합니다.
 * userId로 조회하는 것이 id로 조회하는 것보다 더 자주 사용됩니다.
 */
public interface UserStatusRepository {
    /**
     * 사용자 상태를 저장합니다.
     *
     * Create와 Update 모두에 사용됩니다.
     *
     * @param userStatus 저장할 사용자 상태
     * @return 저장된 사용자 상태
     */
    UserStatus save(UserStatus userStatus);

    /**
     * ID로 사용자 상태를 조회합니다.
     *
     * @param id 사용자 상태 ID (UserStatus 자체의 ID, User ID 아님)
     * @return 조회된 사용자 상태 (없으면 Optional.empty())
     */
    Optional<UserStatus> findById(UUID id);

    /**
     * 사용자 ID로 사용자 상태를 조회합니다.
     *
     * User의 온라인 상태를 확인할 때 주로 사용합니다.
     *
     * [사용 예시]
     * boolean isOnline = userStatusRepository.findByUserId(userId)
     *     .map(UserStatus::isOnline)
     *     .orElse(false);
     *
     * @param userId 사용자 ID (User 엔티티의 ID)
     * @return 조회된 사용자 상태 (없으면 Optional.empty())
     */
    Optional<UserStatus> findByUserId(UUID userId);

    /**
     * 모든 사용자 상태를 조회합니다.
     *
     * 전체 사용자의 온라인 현황 파악에 사용할 수 있습니다.
     *
     * @return 모든 사용자 상태 목록
     */
    List<UserStatus> findAll();

    /**
     * ID로 사용자 상태를 삭제합니다.
     *
     * @param id 삭제할 사용자 상태 ID
     */
    void deleteById(UUID id);

    /**
     * 사용자 ID로 사용자 상태를 삭제합니다.
     *
     * User 삭제 시 연관된 UserStatus도 함께 삭제할 때 사용합니다.
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(UUID userId);

    /**
     * ID로 사용자 상태의 존재 여부를 확인합니다.
     *
     * @param id 사용자 상태 ID
     * @return true: 존재함, false: 존재하지 않음
     */
    boolean existsById(UUID id);

    /**
     * 사용자 ID로 사용자 상태의 존재 여부를 확인합니다.
     *
     * UserStatus 중복 생성 방지에 사용합니다.
     *
     * @param userId 사용자 ID
     * @return true: 존재함, false: 존재하지 않음
     */
    boolean existsByUserId(UUID userId);
}
