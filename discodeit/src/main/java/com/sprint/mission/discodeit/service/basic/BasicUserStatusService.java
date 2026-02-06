package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.request.UserStatusCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserStatusUpdateRequest;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import com.sprint.mission.discodeit.service.UserStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * ========================================
 * UserStatusService 인터페이스의 기본 구현체
 * ========================================
 *
 * 이 클래스는 사용자 온라인 상태 관련 비즈니스 로직을 구현합니다.
 * 사용자가 온라인인지 오프라인인지 추적합니다.
 *
 * [온라인 판정 기준]
 * lastActiveAt이 현재 시간으로부터 5분 이내면 온라인으로 판단합니다.
 * (UserStatus.isOnline() 메서드 참조)
 *
 * [의존하는 Repository]
 * - UserStatusRepository: 사용자 상태 데이터 저장/조회
 * - UserRepository: 사용자 존재 여부 확인
 *
 * [어노테이션 설명]
 * @Service: Spring Bean으로 등록
 * @RequiredArgsConstructor: final 필드에 대한 생성자 자동 생성
 */
@Service
@RequiredArgsConstructor
public class BasicUserStatusService implements UserStatusService {
    /**
     * 사용자 상태 정보를 저장하고 조회하는 리포지토리
     */
    private final UserStatusRepository userStatusRepository;

    /**
     * 사용자 존재 여부 확인을 위한 리포지토리
     */
    private final UserRepository userRepository;

    /**
     * ========================================
     * 사용자 상태를 생성합니다.
     * ========================================
     *
     * [검증 사항]
     * 1. userId의 User가 존재하는지 확인
     * 2. 해당 User의 UserStatus가 이미 있는지 확인 (중복 방지)
     *
     * [참고]
     * 보통 UserService.create()에서 User 생성 시 함께 생성되므로
     * 직접 호출할 일은 많지 않습니다.
     */
    @Override
    public UserStatus create(UserStatusCreateRequest request) {
        // 1단계: User 존재 여부 확인
        if (!userRepository.existsById(request.userId())) {
            throw new NoSuchElementException("User not found: " + request.userId());
        }

        // 2단계: 중복 체크 (한 User당 하나의 UserStatus만)
        if (userStatusRepository.existsByUserId(request.userId())) {
            throw new IllegalArgumentException("UserStatus already exists for user: " + request.userId());
        }

        // UserStatus 생성 및 저장
        UserStatus userStatus = new UserStatus(request.userId(), request.lastAccessAt());
        return userStatusRepository.save(userStatus);
    }

    /**
     * ID로 사용자 상태를 조회합니다.
     *
     * [findById + orElseThrow 패턴]
     * - findById(): Optional 반환 (값이 있으면 of, 없으면 empty)
     * - orElseThrow(): 값이 있으면 반환, 없으면 예외 발생
     *
     * ※ Optional과 람다에 대한 상세 설명은
     *   BasicBinaryContentService.find() 참조
     *
     * @param id 조회할 UserStatus ID
     * @return 조회된 UserStatus 엔티티
     * @throws NoSuchElementException 해당 ID의 UserStatus가 없을 경우
     */
    @Override
    public UserStatus find(UUID id) {
        return userStatusRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("UserStatus not found: " + id));
    }

    /**
     * 사용자 ID로 상태를 조회합니다.
     *
     * User의 온라인 상태를 확인할 때 주로 사용합니다.
     */
    @Override
    public UserStatus findByUserId(UUID userId) {
        return userStatusRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("UserStatus not found for user: " + userId));
    }

    /**
     * 모든 사용자 상태를 조회합니다.
     *
     * @return 모든 UserStatus 목록 (없으면 빈 리스트, null 아님)
     */
    @Override
    public List<UserStatus> findAll() {
        return userStatusRepository.findAll();
    }

    /**
     * 사용자 상태를 업데이트합니다.
     *
     * [조회 → 수정 → 저장 패턴]
     * 1. 기존 데이터 조회 (없으면 예외)
     * 2. 엔티티 값 변경 (update 내부에서 updatedAt도 갱신)
     * 3. 변경된 엔티티 저장 (같은 ID이므로 UPDATE 동작)
     *
     * @param id 수정할 UserStatus ID
     * @param request 수정 요청 (lastAccessAt)
     * @return 수정된 UserStatus 엔티티
     * @throws NoSuchElementException 해당 ID의 UserStatus가 없을 경우
     */
    @Override
    public UserStatus update(UUID id, UserStatusUpdateRequest request) {
        UserStatus userStatus = userStatusRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("UserStatus not found: " + id));

        userStatus.update(request.lastAccessAt());
        return userStatusRepository.save(userStatus);
    }

    /**
     * 사용자 ID로 상태를 업데이트합니다.
     *
     * 사용자 활동 시 마지막 접속 시간을 갱신할 때 주로 사용합니다.
     * (UserStatus ID가 아닌 User ID로 조회)
     */
    @Override
    public UserStatus updateByUserId(UUID userId, UserStatusUpdateRequest request) {
        UserStatus userStatus = userStatusRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("UserStatus not found for user: " + userId));

        userStatus.update(request.lastAccessAt());
        return userStatusRepository.save(userStatus);
    }

    /**
     * 사용자 상태를 삭제합니다.
     *
     * [참고] deleteById는 ID가 없어도 예외를 발생시키지 않습니다.
     *
     * @param id 삭제할 UserStatus ID
     */
    @Override
    public void delete(UUID id) {
        userStatusRepository.deleteById(id);
    }
}
