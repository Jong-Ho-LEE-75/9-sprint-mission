package com.sprint.mission.discodeit.entity;

import lombok.Getter;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * ========================================
 * 모든 엔티티의 공통 속성을 정의하는 기본 엔티티 클래스
 * ========================================
 *
 * 이 클래스는 다른 모든 엔티티 클래스(User, Channel, Message 등)가
 * 상속받는 부모 클래스입니다. 공통적으로 필요한 속성들을 한 곳에서 관리합니다.
 *
 * [왜 BaseEntity를 사용하나요?]
 * 모든 엔티티는 다음과 같은 공통 속성이 필요합니다:
 * - id: 각 데이터를 구분하는 고유 식별자
 * - createdAt: 데이터가 생성된 시간
 * - updatedAt: 데이터가 마지막으로 수정된 시간
 *
 * 이런 공통 속성을 각 클래스에 반복해서 작성하면 코드 중복이 발생합니다.
 * BaseEntity로 한 번만 정의하고 상속받으면 코드 중복을 피할 수 있습니다.
 *
 * [상속(Inheritance)이란?]
 * 자식 클래스가 부모 클래스의 속성과 메서드를 물려받는 것입니다.
 * 예: User extends BaseEntity → User는 id, createdAt, updatedAt을 자동으로 가짐
 *
 * [직렬화(Serialization)란?]
 * 객체를 바이트 스트림으로 변환하여 파일에 저장하거나 네트워크로 전송하는 것입니다.
 * Serializable 인터페이스를 구현하면 Java가 객체를 직렬화할 수 있게 됩니다.
 * 예: User 객체 → 파일로 저장 → 나중에 파일에서 다시 User 객체로 복원
 *
 * [어노테이션 설명]
 * @Getter (Lombok): 모든 필드에 대한 getter 메서드를 자동 생성합니다.
 *   - getId(), getCreatedAt(), getUpdatedAt() 메서드가 자동으로 만들어집니다.
 *   - 직접 작성하면 코드가 길어지는데, Lombok이 대신 해줍니다.
 */
@Getter
public class BaseEntity implements Serializable {
    /**
     * 직렬화 버전 UID (Unique Identifier)
     *
     * 직렬화된 객체의 버전을 식별하는 고유 번호입니다.
     * 클래스 구조가 변경되면 이 값을 변경해야 합니다.
     * 그렇지 않으면 이전에 저장된 데이터를 읽을 때 오류가 발생할 수 있습니다.
     *
     * @Serial 어노테이션은 이 필드가 직렬화 관련 필드임을 표시합니다.
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 엔티티의 고유 식별자 (Primary Key)
     *
     * UUID (Universally Unique Identifier)는 전 세계적으로 유일한 ID를 생성합니다.
     * 형식: 550e8400-e29b-41d4-a716-446655440000 (36자리 문자열)
     *
     * [왜 UUID를 사용하나요?]
     * 1. 중복 가능성이 거의 없음 (2^122가지 조합 가능)
     * 2. 서버 여러 대에서 동시에 ID를 생성해도 충돌하지 않음
     * 3. ID 값으로 데이터의 생성 순서를 추측할 수 없어 보안에 유리
     *
     * protected: 이 클래스와 자식 클래스에서만 접근 가능
     */
    protected UUID id;

    /**
     * 엔티티 생성 일시
     *
     * Instant는 특정 시점을 나타내는 Java 8+ 클래스입니다.
     * UTC(협정 세계시) 기준으로 저장되어 시간대에 관계없이 일관된 시간을 나타냅니다.
     * 예: 2024-01-15T10:30:00Z (Z는 UTC를 의미)
     */
    protected Instant createdAt;

    /**
     * 엔티티 마지막 수정 일시
     *
     * 데이터가 수정될 때마다 이 값이 갱신됩니다.
     * 이를 통해 데이터의 최신 여부를 확인할 수 있습니다.
     */
    protected Instant updatedAt;

    /**
     * ========================================
     * 기본 생성자
     * ========================================
     *
     * 새로운 엔티티가 생성될 때 자동으로 호출됩니다.
     * UUID를 생성하고 생성일시와 수정일시를 현재 시간으로 초기화합니다.
     *
     * [생성자란?]
     * 객체가 만들어질 때 자동으로 실행되는 특별한 메서드입니다.
     * 객체의 초기 상태를 설정하는 데 사용됩니다.
     *
     * 예: User user = new User(); → BaseEntity 생성자가 먼저 실행됨
     */
    public BaseEntity() {
        // UUID.randomUUID()는 전 세계적으로 유일한 ID를 무작위로 생성합니다.
        this.id = UUID.randomUUID();

        // Instant.now()는 현재 시간을 UTC 기준으로 반환합니다.
        Instant now = Instant.now();

        // 생성 시점에는 createdAt과 updatedAt이 동일합니다.
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * ========================================
     * 수정 일시 갱신 메서드
     * ========================================
     *
     * 엔티티 수정 시 이 메서드를 호출하여 updatedAt을 현재 시간으로 업데이트합니다.
     *
     * [protected 접근 제한자란?]
     * - public: 모든 클래스에서 접근 가능
     * - protected: 같은 패키지 또는 자식 클래스에서만 접근 가능
     * - default(없음): 같은 패키지에서만 접근 가능
     * - private: 해당 클래스 내에서만 접근 가능
     *
     * protected를 사용한 이유:
     * 외부에서 직접 호출하면 안 되고, 자식 클래스의 update 메서드 내에서만
     * 호출되어야 하기 때문입니다.
     */
    protected void updateTimeStamp() {
        this.updatedAt = Instant.now();
    }
}
