package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.request.LoginRequest;
import com.sprint.mission.discodeit.dto.response.UserResponse;

/**
 * ========================================
 * 인증 서비스 인터페이스
 * ========================================
 *
 * 이 인터페이스는 사용자 인증 관련 비즈니스 로직을 정의합니다.
 * 로그인, 로그아웃 등의 기능을 제공합니다.
 *
 * [인증(Authentication)이란?]
 * "당신이 누구인지" 확인하는 과정입니다.
 * 예: 로그인 (username + password로 신원 확인)
 *
 * [인가(Authorization)란?]
 * "당신이 무엇을 할 수 있는지" 확인하는 과정입니다.
 * 예: 관리자만 삭제 가능 (권한 확인)
 *
 * [현재 구현]
 * 단순 로그인만 구현되어 있습니다.
 * - 세션/토큰 관리 없음
 * - 비밀번호 암호화 없음 (평문 비교)
 *
 * [실제 프로덕션에서는]
 * - Spring Security 사용
 * - JWT 또는 Session 기반 인증
 * - BCrypt로 비밀번호 해싱
 * - OAuth2 (Google, GitHub 로그인)
 *
 * [구현체]
 * BasicAuthService: 기본 인증 서비스 구현
 */
public interface AuthService {
    /**
     * 사용자 로그인을 처리합니다.
     *
     * [처리 과정]
     * 1. username으로 사용자 조회
     * 2. 비밀번호 일치 확인
     * 3. 성공 시 사용자 정보 반환
     *
     * @param request 로그인 요청 (username, password)
     * @return 로그인한 사용자 정보 (온라인 상태 포함)
     * @throws java.util.NoSuchElementException 사용자가 없거나 비밀번호가 틀린 경우
     */
    UserResponse login(LoginRequest request);
}
