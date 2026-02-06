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
     * [코드 상세 분석 - map과 orElse 조합]
     *
     * ----------------------------------------
     * 온라인 상태 확인 코드
     * ----------------------------------------
     * boolean isOnline = userStatusRepository.findByUserId(user.getId())
     *         .map(UserStatus::isOnline)
     *         .orElse(false);
     *
     * 이 코드는 Optional의 map과 orElse를 조합한 패턴입니다.
     *
     * [1단계: findByUserId(...)]
     * Optional<UserStatus>를 반환합니다.
     * - 값이 있으면: Optional.of(UserStatus객체)
     * - 값이 없으면: Optional.empty()
     *
     * [2단계: .map(UserStatus::isOnline)]
     * Optional 안의 값을 변환합니다.
     *
     * [map()이란?]
     * Optional 안에 값이 있을 때만 함수를 적용합니다.
     * - Optional.of(값).map(함수) → Optional.of(함수(값))
     * - Optional.empty().map(함수) → Optional.empty() (함수 실행 안 함)
     *
     * [예시]
     * // 값이 있는 경우
     * Optional.of(UserStatus객체)
     *     .map(UserStatus::isOnline)
     *     → Optional.of(true) 또는 Optional.of(false)
     *
     * // 값이 없는 경우
     * Optional.empty()
     *     .map(UserStatus::isOnline)
     *     → Optional.empty() (isOnline()이 호출되지 않음!)
     *
     * [3단계: .orElse(false)]
     * Optional에서 최종 값을 추출합니다.
     * - Optional에 값이 있으면: 그 값 (true 또는 false)
     * - Optional이 비어있으면: false (기본값)
     *
     * [전체 동작 흐름]
     * // UserStatus가 존재하고 온라인인 경우
     * findByUserId(...) → Optional.of(UserStatus)
     *     .map(isOnline) → Optional.of(true)
     *     .orElse(false) → true
     *
     * // UserStatus가 존재하고 오프라인인 경우
     * findByUserId(...) → Optional.of(UserStatus)
     *     .map(isOnline) → Optional.of(false)
     *     .orElse(false) → false
     *
     * // UserStatus가 없는 경우
     * findByUserId(...) → Optional.empty()
     *     .map(isOnline) → Optional.empty() (실행 안 됨)
     *     .orElse(false) → false
     *
     * [이 패턴의 장점]
     * // 전통적인 방식
     * UserStatus status = userStatusRepository.findByUserId(id);
     * boolean isOnline;
     * if (status != null) {
     *     isOnline = status.isOnline();
     * } else {
     *     isOnline = false;
     * }
     *
     * // Optional 방식 (한 줄로 표현)
     * boolean isOnline = repository.findByUserId(id)
     *         .map(UserStatus::isOnline)
     *         .orElse(false);
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
        // map(): UserStatus가 있으면 isOnline() 호출, 없으면 건너뜀
        // orElse(false): UserStatus가 없으면 false 반환
        boolean isOnline = userStatusRepository.findByUserId(user.getId())
                .map(UserStatus::isOnline)  // Optional<UserStatus> → Optional<Boolean>
                .orElse(false);             // Optional<Boolean> → boolean (기본값: false)

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
