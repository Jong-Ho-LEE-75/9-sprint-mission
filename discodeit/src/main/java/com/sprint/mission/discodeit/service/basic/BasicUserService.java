package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserUpdateRequest;
import com.sprint.mission.discodeit.dto.response.UserDto;
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
 * UserService 인터페이스의 기본 구현체.
 * 사용자의 생성, 조회, 수정, 삭제 및 프로필 이미지 관리 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
public class BasicUserService implements UserService {

    /**
     * 사용자 정보를 저장하고 조회하는 리포지토리
     */
    private final UserRepository userRepository;

    /**
     * 프로필 이미지를 저장하고 조회하는 리포지토리
     */
    private final BinaryContentRepository binaryContentRepository;

    /**
     * 사용자 상태 정보를 저장하고 조회하는 리포지토리
     */
    private final UserStatusRepository userStatusRepository;

    /**
     * 사용자를 생성합니다.
     * 사용자 이름과 이메일 중복을 확인하고, 프로필 이미지가 있는 경우 함께 저장합니다.
     *
     * @param request 사용자 생성 요청 정보
     * @param profileRequest 프로필 이미지 정보 (선택 사항)
     * @return 생성된 사용자 정보
     * @throws IllegalArgumentException 사용자 이름 또는 이메일이 이미 존재할 경우
     */
    @Override
    public UserResponse create(UserCreateRequest request, BinaryContentCreateRequest profileRequest) {
        // 1. 사용자 이름 중복 체크
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("이 사용자 이름은 이미 존재해요!: " + request.username());
        }

        // 2. 이메일 중복 체크
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이 이메일은 이미 존재해요!: " + request.email());
        }

        // 3. 프로필 이미지 저장 (선택 사항)
        UUID profileId = null;
        if (profileRequest != null) {
            BinaryContent profile = new BinaryContent(
                    profileRequest.fileName(),
                    profileRequest.contentType(),
                    profileRequest.data()
            );
            profileId = binaryContentRepository.save(profile).getId();
        }

        // 4. User 객체 생성
        User user = new User(
                request.username(),
                request.email(),
                request.password(),
                profileId
        );
        User savedUser = userRepository.save(user);

        // 5. UserStatus 객체 생성 및 저장
        UserStatus userStatus = new UserStatus(savedUser.getId(), Instant.now());
        userStatusRepository.save(userStatus);

        // 6. 최종 결과 반환
        return toUserResponse(savedUser, true);
    }

    @Override
    public UserResponse find(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));

        boolean isOnline = getOnlineStatus(user.getId());
        return toUserResponse(user, isOnline);
    }

    @Override
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(user -> toUserResponse(user, getOnlineStatus(user.getId())))
                .toList();
    }

    @Override
    public List<UserDto> findAllAsDto() {
        return userRepository.findAll().stream()
                .map(user -> {
                    boolean isOnline = getOnlineStatus(user.getId());
                    return new UserDto(
                            user.getId(),
                            user.getCreatedAt(),
                            user.getUpdatedAt(),
                            user.getUsername(),
                            user.getEmail(),
                            user.getProfileId(),
                            isOnline
                    );
                })
                .toList();
    }

    /**
     * 사용자 정보를 수정합니다.
     * 프로필 이미지가 변경되는 경우 기존 이미지를 삭제하고 새 이미지를 저장합니다.
     *
     * @param id 수정할 사용자 ID
     * @param request 사용자 수정 요청 정보
     * @param profileRequest 새 프로필 이미지 정보 (선택 사항)
     * @return 수정된 사용자 정보
     * @throws NoSuchElementException 사용자를 찾을 수 없을 경우
     */
    @Override
    public UserResponse update(UUID id, UserUpdateRequest request, BinaryContentCreateRequest profileRequest) {
        // 1. 수정할 사용자 먼저 찾기
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));

        // 2. 프로필 이미지 교체 (선택 사항)
        UUID newProfileId = user.getProfileId();
        if (profileRequest != null) {
            if (user.getProfileId() != null) {
                binaryContentRepository.deleteById(user.getProfileId());
            }
            BinaryContent profile = new BinaryContent(
                    profileRequest.fileName(),
                    profileRequest.contentType(),
                    profileRequest.data()
            );
            newProfileId = binaryContentRepository.save(profile).getId();
        }

        // 3. 사용자 정보 업데이트
        user.update(request.username(), request.email(), request.password(), newProfileId);

        // 4. 변경된 정보 저장
        User savedUser = userRepository.save(user);

        // 5. 최종 결과 반환
        boolean isOnline = getOnlineStatus(savedUser.getId());
        return toUserResponse(savedUser, isOnline);
    }

    /**
     * 사용자를 삭제합니다.
     * 연관된 프로필 이미지와 상태 정보도 함께 삭제합니다.
     *
     * @param id 삭제할 사용자 ID
     * @throws NoSuchElementException 사용자를 찾을 수 없을 경우
     */
    @Override
    public void delete(UUID id) {
        // 1. 삭제할 사용자 먼저 찾기
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));

        // 2. 연관된 데이터들 먼저 삭제하기
        if (user.getProfileId() != null) {
            binaryContentRepository.deleteById(user.getProfileId());
        }
        userStatusRepository.deleteByUserId(id);

        // 3. 최종적으로 사용자 삭제
        userRepository.deleteById(id);
    }

    /**
     * 사용자의 온라인 상태를 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 온라인 여부 (상태 정보가 없으면 false)
     */
    private boolean getOnlineStatus(UUID userId) {
        return userStatusRepository.findByUserId(userId)
                .map(UserStatus::isOnline)
                .orElse(false);
    }

    /**
     * User 엔티티를 UserResponse DTO로 변환합니다.
     * 비밀번호는 포함하지 않습니다.
     *
     * @param user 변환할 User 엔티티
     * @param isOnline 온라인 상태
     * @return UserResponse DTO
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