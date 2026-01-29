package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserUpdateRequest;
import com.sprint.mission.discodeit.dto.response.UserResponse;
import java.util.List;
import java.util.UUID;

/**
 * ========================================
 * 사용자 서비스 인터페이스
 * ========================================
 *
 * 이 인터페이스는 사용자 관련 비즈니스 로직을 정의합니다.
 * 회원가입, 사용자 조회, 정보 수정, 탈퇴 등의 기능을 제공합니다.
 *
 * [Service 계층의 역할]
 * 1. 비즈니스 로직 처리: 입력값 검증, 중복 체크 등
 * 2. 트랜잭션 관리: 여러 작업을 하나의 단위로 처리
 * 3. 여러 Repository 조합: User + BinaryContent + UserStatus
 * 4. DTO ↔ Entity 변환: Request DTO → Entity, Entity → Response DTO
 *
 * [계층 구조]
 * Controller → Service (인터페이스) → ServiceImpl (구현체) → Repository
 *
 * [왜 인터페이스로 정의하나요?]
 * 1. 추상화: Controller는 구현 세부사항을 몰라도 됨
 * 2. 테스트 용이: Mock 객체로 쉽게 대체 가능
 * 3. 다형성: 다양한 구현체로 교체 가능
 *
 * [구현체]
 * BasicUserService: 기본 사용자 서비스 구현
 */
public interface UserService {
    /**
     * 새 사용자를 생성합니다. (회원가입)
     *
     * [처리 과정]
     * 1. username 중복 체크
     * 2. email 중복 체크
     * 3. 프로필 이미지 저장 (선택)
     * 4. User 엔티티 생성 및 저장
     * 5. UserStatus 엔티티 생성 및 저장
     * 6. UserResponse DTO 반환
     *
     * @param request        사용자 생성 요청 (username, email, password)
     * @param profileRequest 프로필 이미지 요청 (선택, null 가능)
     * @return 생성된 사용자 정보
     * @throws IllegalArgumentException username 또는 email이 중복될 경우
     */
    UserResponse create(UserCreateRequest request, BinaryContentCreateRequest profileRequest);

    /**
     * ID로 사용자를 조회합니다.
     *
     * @param id 사용자 ID
     * @return 조회된 사용자 정보
     * @throws java.util.NoSuchElementException 사용자가 없을 경우
     */
    UserResponse find(UUID id);

    /**
     * 모든 사용자를 조회합니다.
     *
     * @return 모든 사용자 목록 (각 사용자의 온라인 상태 포함)
     */
    List<UserResponse> findAll();

    /**
     * 사용자 정보를 수정합니다.
     *
     * [처리 과정]
     * 1. 기존 사용자 조회
     * 2. 새 프로필 이미지가 있으면: 기존 이미지 삭제 → 새 이미지 저장
     * 3. User 정보 업데이트
     * 4. UserResponse DTO 반환
     *
     * @param id             수정할 사용자 ID
     * @param request        수정 요청 (null인 필드는 변경하지 않음)
     * @param profileRequest 새 프로필 이미지 (선택, null이면 이미지 변경 안 함)
     * @return 수정된 사용자 정보
     * @throws java.util.NoSuchElementException 사용자가 없을 경우
     */
    UserResponse update(UUID id, UserUpdateRequest request, BinaryContentCreateRequest profileRequest);

    /**
     * 사용자를 삭제합니다. (회원 탈퇴)
     *
     * [처리 과정 (Cascading Delete)]
     * 1. 기존 사용자 조회
     * 2. 프로필 이미지 삭제 (있는 경우)
     * 3. UserStatus 삭제
     * 4. User 삭제
     *
     * @param id 삭제할 사용자 ID
     * @throws java.util.NoSuchElementException 사용자가 없을 경우
     */
    void delete(UUID id);
}
