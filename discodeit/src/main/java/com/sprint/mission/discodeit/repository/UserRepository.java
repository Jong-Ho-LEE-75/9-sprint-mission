package com.sprint.mission.discodeit.repository;

import com.sprint.mission.discodeit.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ========================================
 * 사용자 저장소 인터페이스
 * ========================================
 *
 * 이 인터페이스는 User 엔티티의 영속성(저장/조회/삭제)을 담당합니다.
 * 데이터를 어디에 저장할지(메모리, 파일, DB)는 구현체가 결정합니다.
 *
 * [Repository 패턴이란?]
 * 데이터 저장소에 접근하는 로직을 추상화하는 디자인 패턴입니다.
 * Service는 Repository 인터페이스만 알면 되고,
 * 실제 저장 방식(JCF, 파일, JPA)은 몰라도 됩니다.
 *
 * [왜 인터페이스로 정의하나요?]
 * 1. 추상화: Service는 "데이터 저장"만 알고, "어떻게" 저장하는지 몰라도 됨
 * 2. 교체 용이: JCF → File → JPA로 쉽게 변경 가능
 * 3. 테스트 용이: Mock 객체로 쉽게 대체 가능
 *
 * [구현체]
 * - JCFUserRepository: Java Collection Framework (HashMap) 사용, 메모리에 저장
 * - FileUserRepository: 파일 시스템에 직렬화하여 저장
 *
 * [설정 방법]
 * application.properties에서 설정:
 * discodeit.repository.type=jcf  → JCFUserRepository 사용
 * discodeit.repository.type=file → FileUserRepository 사용
 *
 * [CRUD 작업]
 * - Create: save()
 * - Read: findById(), findByUsername(), findByEmail(), findAll()
 * - Update: save() (기존 ID로 저장하면 업데이트)
 * - Delete: deleteById()
 */
public interface UserRepository {
    /**
     * 사용자를 저장합니다.
     *
     * 새 사용자 저장(Create)과 기존 사용자 수정(Update) 모두에 사용됩니다.
     * - 새 ID로 저장 → Create
     * - 기존 ID로 저장 → Update (덮어쓰기)
     *
     * @param user 저장할 사용자 엔티티
     * @return 저장된 사용자 엔티티 (동일 객체 반환)
     */
    User save(User user);

    /**
     * ID로 사용자를 조회합니다.
     *
     * Optional을 반환하여 사용자가 존재하지 않을 때 null 대신
     * Optional.empty()를 반환합니다.
     *
     * [Optional 사용법]
     * Optional<User> optUser = userRepository.findById(id);
     *
     * // 방법 1: orElseThrow (없으면 예외)
     * User user = optUser.orElseThrow(() -> new NoSuchElementException("User not found"));
     *
     * // 방법 2: orElse (없으면 기본값)
     * User user = optUser.orElse(defaultUser);
     *
     * // 방법 3: ifPresent (있을 때만 실행)
     * optUser.ifPresent(user -> System.out.println(user.getUsername()));
     *
     * @param id 사용자 ID (UUID)
     * @return 조회된 사용자 (없으면 Optional.empty())
     */
    Optional<User> findById(UUID id);

    /**
     * 사용자명으로 사용자를 조회합니다.
     *
     * 로그인 시 username으로 사용자를 찾을 때 사용합니다.
     * username은 고유하므로 결과는 하나만 있거나 없습니다.
     *
     * [사용 예시 - 로그인]
     * User user = userRepository.findByUsername(loginRequest.username())
     *     .orElseThrow(() -> new NoSuchElementException("Invalid username"));
     *
     * @param username 사용자명
     * @return 조회된 사용자 (없으면 Optional.empty())
     */
    Optional<User> findByUsername(String username);

    /**
     * 이메일로 사용자를 조회합니다.
     *
     * 이메일 중복 체크나 이메일로 사용자 찾기에 사용합니다.
     *
     * @param email 이메일 주소
     * @return 조회된 사용자 (없으면 Optional.empty())
     */
    Optional<User> findByEmail(String email);

    /**
     * 모든 사용자를 조회합니다.
     *
     * 사용자 목록 조회에 사용합니다.
     * 데이터가 많으면 페이징을 고려해야 합니다.
     *
     * @return 모든 사용자 목록 (없으면 빈 리스트)
     */
    List<User> findAll();

    /**
     * ID로 사용자를 삭제합니다.
     *
     * 삭제 전 연관된 데이터(UserStatus, 프로필 이미지)를
     * 먼저 삭제해야 할 수 있습니다. (Service에서 처리)
     *
     * @param id 삭제할 사용자 ID
     */
    void deleteById(UUID id);

    /**
     * ID로 사용자의 존재 여부를 확인합니다.
     *
     * 조회보다 가볍게 존재 여부만 확인할 때 사용합니다.
     *
     * @param id 사용자 ID
     * @return true: 존재함, false: 존재하지 않음
     */
    boolean existsById(UUID id);

    /**
     * 사용자명으로 존재 여부를 확인합니다. (중복 체크용)
     *
     * 회원가입 시 username 중복 체크에 사용합니다.
     *
     * [사용 예시]
     * if (userRepository.existsByUsername(request.username())) {
     *     throw new IllegalArgumentException("이미 사용 중인 사용자명입니다.");
     * }
     *
     * @param username 사용자명
     * @return true: 이미 존재함 (사용 불가), false: 사용 가능
     */
    boolean existsByUsername(String username);

    /**
     * 이메일로 존재 여부를 확인합니다. (중복 체크용)
     *
     * 회원가입 시 email 중복 체크에 사용합니다.
     *
     * @param email 이메일 주소
     * @return true: 이미 존재함 (사용 불가), false: 사용 가능
     */
    boolean existsByEmail(String email);
}
