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
 * ========================================
 * AuthService 인터페이스의 기본 구현체
 * ========================================
 *
 * 이 클래스는 사용자 인증(로그인) 기능을 구현합니다.
 *
 * [현재 구현의 한계]
 * - 비밀번호를 평문으로 비교 (실제로는 해시 비교 필요)
 * - 세션/토큰 관리 없음 (실제로는 JWT 또는 세션 필요)
 * - 로그인 실패 횟수 제한 없음 (실제로는 필요)
 *
 * [보안 개선 방향]
 * 1. BCryptPasswordEncoder로 비밀번호 해싱
 * 2. Spring Security + JWT 토큰 인증
 * 3. 로그인 시도 횟수 제한 (계정 잠금)
 * 4. 2단계 인증(2FA)
 *
 * [어노테이션 설명]
 * @Service: Spring Bean으로 등록
 * @RequiredArgsConstructor: final 필드에 대한 생성자 자동 생성
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
     * ========================================
     * 사용자 로그인 처리
     * ========================================
     *
     * [처리 순서]
     * 1. username으로 사용자 조회 → 없으면 예외
     * 2. 비밀번호 일치 확인 → 불일치하면 예외
     * 3. 온라인 상태 조회
     * 4. UserResponse 반환
     *
     * [보안 주의사항]
     * 예외 메시지를 "Invalid username or password"로 통일합니다.
     * "사용자 없음"과 "비밀번호 틀림"을 구분하면
     * 공격자가 유효한 username을 알아낼 수 있습니다.
     *
     * @param request 로그인 요청 (username, password)
     * @return 로그인한 사용자 정보
     * @throws NoSuchElementException username이 없거나 password가 틀린 경우
     */
    @Override
    public UserResponse login(LoginRequest request) {
        // 1단계: username으로 User 조회
        // orElseThrow: Optional이 비어있으면 예외 발생
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new NoSuchElementException("Invalid username or password"));

        // 2단계: 비밀번호 확인
        // [주의] 평문 비교는 보안상 위험합니다!
        // 실제로는: if (!passwordEncoder.matches(request.password(), user.getPassword()))
        if (!user.getPassword().equals(request.password())) {
            throw new NoSuchElementException("Invalid username or password");
        }

        // 3단계: 온라인 상태 확인
        // UserStatus가 있으면 isOnline() 결과, 없으면 false
        boolean isOnline = userStatusRepository.findByUserId(user.getId())
                .map(UserStatus::isOnline)
                .orElse(false);

        // 4단계: UserResponse DTO 생성 및 반환
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
