package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.request.LoginRequest;
import com.sprint.mission.discodeit.dto.response.UserResponse;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import com.sprint.mission.discodeit.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

/**
 * AuthService 인터페이스의 기본 구현체.
 * 사용자 인증 및 로그인 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
public class BasicAuthService implements AuthService {
    /**
     * 사용자 정보를 저장하고 조회하는 리포지토리
     */
    private final UserRepository userRepository;

    /**
     * 사용자 상태 정보를 저장하고 조회하는 리포지토리
     */
    private final UserStatusRepository userStatusRepository;

    /**
     * 사용자 로그인을 처리합니다.
     *
     * @param request 로그인 요청 정보 (username, password)
     * @return 로그인한 사용자의 정보
     * @throws NoSuchElementException 사용자를 찾을 수 없거나 비밀번호가 일치하지 않을 경우
     */
    @Override
    public UserResponse login(LoginRequest request) {
        // username으로 User 조회
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new NoSuchElementException("Invalid username or password"));

        // password 확인
        if (!user.getPassword().equals(request.password())) {
            throw new NoSuchElementException("Invalid username or password");
        }

        // 온라인 상태 확인
        boolean isOnline = userStatusRepository.findByUserId(user.getId())
                .map(UserStatus::isOnline)
                .orElse(false);

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