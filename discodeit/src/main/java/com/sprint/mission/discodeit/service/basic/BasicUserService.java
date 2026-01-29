package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserUpdateRequest;
import com.sprint.mission.discodeit.dto.response.UserResponse;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import com.sprint.mission.discodeit.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * ========================================
 * UserService 인터페이스의 기본 구현체
 * ========================================
 *
 * 이 클래스는 사용자 관련 비즈니스 로직을 실제로 구현합니다.
 * 회원가입, 사용자 조회, 정보 수정, 탈퇴 기능을 제공합니다.
 *
 * [의존하는 Repository]
 * - UserRepository: 사용자 데이터 저장/조회
 * - BinaryContentRepository: 프로필 이미지 저장/조회
 * - UserStatusRepository: 사용자 온라인 상태 저장/조회
 *
 * [어노테이션 설명]
 * @Service: 이 클래스가 서비스 계층의 Bean임을 Spring에 알림
 *           Spring이 자동으로 Bean으로 등록하고 관리함
 *
 * @RequiredArgsConstructor (Lombok):
 *   final 필드에 대한 생성자를 자동으로 생성합니다.
 *   Spring은 이 생성자를 통해 의존성을 주입합니다 (생성자 주입).
 *
 *   자동 생성되는 코드:
 *   public BasicUserService(UserRepository userRepository,
 *                           BinaryContentRepository binaryContentRepository,
 *                           UserStatusRepository userStatusRepository) {
 *       this.userRepository = userRepository;
 *       this.binaryContentRepository = binaryContentRepository;
 *       this.userStatusRepository = userStatusRepository;
 *   }
 */
@Service
@RequiredArgsConstructor
public class BasicUserService implements UserService {

    /**
     * 사용자 정보를 저장하고 조회하는 리포지토리
     *
     * final 키워드: 생성자에서 한 번 주입되면 변경 불가
     * 이렇게 하면 불변성이 보장되어 안전합니다.
     */
    private final UserRepository userRepository;

    /**
     * 프로필 이미지를 저장하고 조회하는 리포지토리
     */
    private final BinaryContentRepository binaryContentRepository;

    /**
     * 사용자 상태(온라인/오프라인) 정보를 저장하고 조회하는 리포지토리
     */
    private final UserStatusRepository userStatusRepository;

    /**
     * ========================================
     * 사용자 생성 (회원가입)
     * ========================================
     *
     * [처리 순서]
     * 1. username 중복 체크 → 이미 있으면 예외
     * 2. email 중복 체크 → 이미 있으면 예외
     * 3. 프로필 이미지 저장 (선택)
     * 4. User 엔티티 생성 및 저장
     * 5. UserStatus 엔티티 생성 및 저장 (회원가입 시 자동 생성)
     * 6. UserResponse DTO로 변환하여 반환
     */
    @Override
    public UserResponse create(UserCreateRequest request, BinaryContentCreateRequest profileRequest) {
        // 1단계: 사용자명 중복 체크
        // 이미 같은 username이 있으면 회원가입 거부
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("이 사용자 이름은 이미 존재해요!: " + request.username());
        }

        // 2단계: 이메일 중복 체크
        // 이미 같은 email이 있으면 회원가입 거부
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이 이메일은 이미 존재해요!: " + request.email());
        }

        // 3단계: 프로필 이미지 저장 (선택 사항)
        // profileRequest가 null이 아니면 이미지를 저장하고 ID를 받음
        UUID profileId = null;
        if (profileRequest != null) {
            BinaryContent profile = new BinaryContent(
                    profileRequest.fileName(),
                    profileRequest.contentType(),
                    profileRequest.data()
            );
            // save()는 저장된 엔티티를 반환하므로 바로 getId() 호출 가능
            profileId = binaryContentRepository.save(profile).getId();
        }

        // 4단계: User 엔티티 생성 및 저장
        User user = new User(
                request.username(),
                request.email(),
                request.password(),
                profileId  // 프로필 이미지가 없으면 null
        );
        User savedUser = userRepository.save(user);

        // 5단계: UserStatus 생성 및 저장
        // 회원가입 시점을 마지막 접속 시간으로 설정 → 온라인 상태로 시작
        UserStatus userStatus = new UserStatus(savedUser.getId(), Instant.now());
        userStatusRepository.save(userStatus);

        // 6단계: UserResponse DTO로 변환하여 반환
        // 방금 생성했으므로 isOnline은 true
        return toUserResponse(savedUser, true);
    }

    @Override
    public UserResponse find(UUID id) {
        // ID로 사용자 조회, 없으면 예외 발생
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));

        // 온라인 상태 조회
        boolean isOnline = getOnlineStatus(user.getId());
        return toUserResponse(user, isOnline);
    }

    @Override
    public List<UserResponse> findAll() {
        // 모든 사용자를 조회하고, 각각 온라인 상태와 함께 DTO로 변환
        return userRepository.findAll().stream()
                .map(user -> toUserResponse(user, getOnlineStatus(user.getId())))
                .toList();  // Java 16+의 toList() - 불변 리스트 반환
    }

    /**
     * ========================================
     * 사용자 정보 수정
     * ========================================
     *
     * [처리 순서]
     * 1. 수정할 사용자 조회 → 없으면 예외
     * 2. 프로필 이미지 교체 (선택)
     *    - 새 이미지가 있으면: 기존 이미지 삭제 → 새 이미지 저장
     * 3. User 정보 업데이트 (null이 아닌 필드만)
     * 4. 변경된 정보 저장
     * 5. UserResponse DTO로 변환하여 반환
     */
    @Override
    public UserResponse update(UUID id, UserUpdateRequest request, BinaryContentCreateRequest profileRequest) {
        // 1단계: 수정할 사용자 먼저 찾기
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));

        // 2단계: 프로필 이미지 교체 (선택 사항)
        UUID newProfileId = user.getProfileId();  // 기본값: 기존 프로필 유지
        if (profileRequest != null) {
            // 기존 프로필 이미지가 있으면 삭제
            if (user.getProfileId() != null) {
                binaryContentRepository.deleteById(user.getProfileId());
            }
            // 새 프로필 이미지 저장
            BinaryContent profile = new BinaryContent(
                    profileRequest.fileName(),
                    profileRequest.contentType(),
                    profileRequest.data()
            );
            newProfileId = binaryContentRepository.save(profile).getId();
        }

        // 3단계: 사용자 정보 업데이트
        // null이 아닌 필드만 업데이트됨 (User.update() 메서드 참조)
        user.update(request.username(), request.email(), request.password(), newProfileId);

        // 4단계: 변경된 정보 저장
        User savedUser = userRepository.save(user);

        // 5단계: 최종 결과 반환
        boolean isOnline = getOnlineStatus(savedUser.getId());
        return toUserResponse(savedUser, isOnline);
    }

    /**
     * ========================================
     * 사용자 삭제 (회원 탈퇴)
     * ========================================
     *
     * [Cascading Delete - 연쇄 삭제]
     * 사용자를 삭제할 때 연관된 데이터도 함께 삭제해야 합니다.
     * 그렇지 않으면 "고아 데이터"가 남게 됩니다.
     *
     * [삭제 순서]
     * 1. 사용자 조회 → 없으면 예외
     * 2. 프로필 이미지 삭제 (있는 경우)
     * 3. UserStatus 삭제
     * 4. User 삭제
     */
    @Override
    public void delete(UUID id) {
        // 1단계: 삭제할 사용자 먼저 찾기
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));

        // 2단계: 연관된 프로필 이미지 삭제
        if (user.getProfileId() != null) {
            binaryContentRepository.deleteById(user.getProfileId());
        }

        // 3단계: 연관된 UserStatus 삭제
        userStatusRepository.deleteByUserId(id);

        // 4단계: 최종적으로 사용자 삭제
        userRepository.deleteById(id);
    }

    /**
     * ========================================
     * 사용자의 온라인 상태를 조회하는 헬퍼 메서드
     * ========================================
     *
     * [동작 방식]
     * 1. userId로 UserStatus 조회
     * 2. UserStatus가 있으면 isOnline() 호출
     * 3. UserStatus가 없으면 false 반환
     *
     * [Optional 체이닝]
     * .map(): Optional 안의 값을 변환
     * .orElse(): Optional이 비어있을 때 기본값 반환
     */
    private boolean getOnlineStatus(UUID userId) {
        return userStatusRepository.findByUserId(userId)
                .map(UserStatus::isOnline)  // UserStatus가 있으면 isOnline() 호출
                .orElse(false);             // 없으면 false
    }

    /**
     * ========================================
     * User 엔티티를 UserResponse DTO로 변환하는 헬퍼 메서드
     * ========================================
     *
     * [변환 이유]
     * 1. 비밀번호 제외: Entity에는 password가 있지만 응답에서는 제외
     * 2. 온라인 상태 추가: isOnline은 Entity가 아닌 계산된 값
     *
     * [헬퍼 메서드란?]
     * 다른 메서드에서 반복적으로 사용되는 로직을 분리한 private 메서드입니다.
     * 코드 중복을 줄이고 가독성을 높입니다.
     */
    private UserResponse toUserResponse(User user, boolean isOnline) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getProfileId(),
                isOnline,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
