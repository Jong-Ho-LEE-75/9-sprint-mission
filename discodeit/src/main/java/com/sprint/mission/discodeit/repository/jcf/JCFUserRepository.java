package com.sprint.mission.discodeit.repository.jcf;

import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.repository.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * ========================================
 * Java Collection Framework 기반 사용자 저장소 구현체
 * ========================================
 *
 * 이 클래스는 UserRepository 인터페이스의 구현체로,
 * HashMap을 사용하여 메모리에 데이터를 저장합니다.
 *
 * [JCF(Java Collection Framework)란?]
 * Java에서 제공하는 데이터 구조 모음입니다.
 * List, Set, Map 등이 포함됩니다.
 * 이 클래스에서는 HashMap을 사용합니다.
 *
 * [HashMap을 사용하는 이유]
 * 1. 빠른 조회: ID(key)로 O(1) 시간에 데이터 조회 가능
 * 2. 단순함: 별도의 설정 없이 바로 사용 가능
 * 3. 개발/테스트 용이: 데이터베이스 없이 개발 가능
 *
 * [메모리 저장의 한계]
 * - 애플리케이션 종료 시 모든 데이터 삭제됨
 * - 대용량 데이터 처리에 부적합
 * - 프로덕션에서는 데이터베이스 사용 권장
 *
 * [어노테이션 설명]
 * @Repository: 이 클래스가 데이터 저장소 역할임을 Spring에 알림
 *              Spring이 자동으로 Bean으로 등록함
 *
 * @ConditionalOnProperty: 조건부 Bean 등록
 *   - name = "discodeit.repository.type": 체크할 속성 이름
 *   - havingValue = "jcf": 이 값일 때만 Bean 등록
 *   - matchIfMissing = true: 속성이 없으면 기본으로 이 Bean 사용
 *
 * [설정 방법]
 * application.properties:
 * discodeit.repository.type=jcf  (또는 생략하면 기본값)
 */
@Repository
@ConditionalOnProperty(name = "discodeit.repository.type", havingValue = "jcf", matchIfMissing = true)
public class JCFUserRepository implements UserRepository {
    /**
     * 사용자 데이터를 저장하는 HashMap
     *
     * [HashMap 구조]
     * Key: UUID (사용자 ID)
     * Value: User (사용자 객체)
     *
     * [사용 예시]
     * data.put(user.getId(), user);  // 저장
     * data.get(userId);              // 조회
     * data.remove(userId);           // 삭제
     */
    private final Map<UUID, User> data = new HashMap<>();

    /**
     * 사용자를 저장합니다.
     *
     * HashMap의 put 메서드를 사용합니다.
     * - 새 ID: 새로운 데이터 추가 (Create)
     * - 기존 ID: 기존 데이터 덮어쓰기 (Update)
     */
    @Override
    public User save(User user) {
        // put(key, value): key에 해당하는 value 저장
        data.put(user.getId(), user);
        return user;
    }

    /**
     * ID로 사용자를 조회합니다.
     *
     * Optional.ofNullable()을 사용하여 null 안전하게 처리합니다.
     * - 데이터 있음: Optional.of(user)
     * - 데이터 없음: Optional.empty()
     */
    @Override
    public Optional<User> findById(UUID id) {
        // get(key): key에 해당하는 value 반환 (없으면 null)
        // ofNullable: null이면 empty, 아니면 of(value)
        return Optional.ofNullable(data.get(id));
    }

    /**
     * 사용자명으로 사용자를 조회합니다.
     *
     * HashMap은 username으로 직접 조회할 수 없으므로
     * 모든 데이터를 순회하며 찾습니다.
     *
     * [Stream 설명]
     * data.values(): Map의 모든 Value(User)를 Collection으로 반환
     * .stream(): Collection을 Stream으로 변환
     * .filter(): 조건에 맞는 요소만 필터링
     * .findFirst(): 첫 번째 요소를 Optional로 반환
     */
    @Override
    public Optional<User> findByUsername(String username) {
        return data.values().stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst();
    }

    /**
     * 이메일로 사용자를 조회합니다.
     *
     * findByUsername과 동일한 방식으로 동작합니다.
     */
    @Override
    public Optional<User> findByEmail(String email) {
        return data.values().stream()
                .filter(user -> user.getEmail().equals(email))
                .findFirst();
    }

    /**
     * 모든 사용자를 조회합니다.
     *
     * HashMap의 values()를 새 ArrayList로 복사하여 반환합니다.
     * 새 리스트를 만드는 이유: 원본 데이터 보호 (방어적 복사)
     */
    @Override
    public List<User> findAll() {
        // new ArrayList<>(collection): collection의 모든 요소를 복사한 새 ArrayList
        return new ArrayList<>(data.values());
    }

    /**
     * ID로 사용자를 삭제합니다.
     */
    @Override
    public void deleteById(UUID id) {
        // remove(key): key에 해당하는 데이터 삭제
        data.remove(id);
    }

    /**
     * ID로 존재 여부를 확인합니다.
     */
    @Override
    public boolean existsById(UUID id) {
        // containsKey(key): key가 존재하는지 확인
        return data.containsKey(id);
    }

    /**
     * 사용자명으로 존재 여부를 확인합니다.
     *
     * anyMatch: 하나라도 조건에 맞으면 true
     */
    @Override
    public boolean existsByUsername(String username) {
        return data.values().stream()
                .anyMatch(user -> user.getUsername().equals(username));
    }

    /**
     * 이메일로 존재 여부를 확인합니다.
     */
    @Override
    public boolean existsByEmail(String email) {
        return data.values().stream()
                .anyMatch(user -> user.getEmail().equals(email));
    }
}
