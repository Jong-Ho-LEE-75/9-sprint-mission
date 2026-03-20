# 강사님 코드 vs 현재 코드 비교 분석

> 분석 기준일: 2026-03-05
> 강사님 코드(참고용)와 현재 구현 코드의 차이를 비교하여 적용 여부를 결정한 내용을 기록합니다.

---

## 1. 적용한 개선 사항

### 1-1. `@NoArgsConstructor(access = AccessLevel.PROTECTED)` — User, BinaryContent

**강사님 코드:**
```java
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseUpdatableEntity { ... }
```

**기존 현재 코드:**
```java
@NoArgsConstructor   // public 기본 생성자
public class User extends BaseUpdatableEntity { ... }
```

**적용 이유:**
JPA 기본 생성자는 `protected` 이상이면 동작한다.
`public`으로 두면 JPA 외의 코드에서 `new User()`로 빈 객체를 실수로 생성할 수 있어 잘못된 상태의 객체가 만들어질 위험이 있다.
`PROTECTED`로 제한하면 패키지 외부에서 직접 생성자 호출이 차단되므로 안전하다.

**적용 파일:**
- `entity/User.java`
- `entity/BinaryContent.java`

---

## 2. 적용하지 않은 것들 (현재 코드가 더 나은 이유)

### 2-1. `BasicUserService.delete()` — 강사님 코드의 조건 반전 버그

**강사님 코드:**
```java
@Transactional
@Override
public void delete(UUID userId) {
    if (userRepository.existsById(userId)) {   // ← 버그: 존재하면 예외를 던짐
        throw new NoSuchElementException("User with id " + userId + " not found");
    }
    userRepository.deleteById(userId);
}
```

**문제점:**
`existsById()`가 `true`를 반환할 때(사용자가 존재할 때) 예외를 던진다.
즉, 사용자가 있으면 삭제 못 하고, 없을 때만 삭제를 시도하는 논리적 오류다.
실제로 이 코드를 실행하면 어떤 사용자도 삭제할 수 없다.

**현재 코드 (올바른 방식):**
```java
@Transactional
@Override
public void delete(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NoSuchElementException("User with id " + userId + " not found"));

    if (user.getProfile() != null) {
        UUID profileId = user.getProfile().getId();
        user.clearProfile();
        binaryContentRepository.flush();
        binaryContentRepository.deleteById(profileId);
        binaryContentStorage.delete(profileId);   // 파일도 같이 삭제
    }
    userRepository.delete(user);
}
```

**현재 코드가 더 나은 이유:**
- 조건 반전 버그 없음
- 프로필 이미지 파일까지 함께 삭제 (`binaryContentStorage.delete()`)
- FK 제약 순서 보장 (`clearProfile()` → `flush()` → `deleteById()`)

---

### 2-2. `BasicUserService.create()` — 강사님 코드의 UserStatus 미저장 버그

**강사님 코드:**
```java
User user = new User(username, email, password, nullableProfile);
Instant now = Instant.now();
UserStatus userStatus = new UserStatus(user, now);   // ← 생성만 하고 저장 안 함

userRepository.save(user);
return userMapper.toDto(user);
```

**문제점:**
`UserStatus` 객체를 생성했지만 `userStatusRepository.save(userStatus)`를 호출하지 않는다.
결과적으로 DB에 UserStatus 레코드가 없어서 이후 `userMapper.toDto(user)` 에서
`user.getStatus()` 가 null이 되어 NPE 또는 `online: null` 반환이 발생한다.

**현재 코드 (올바른 방식):**
```java
User user = new User(username, email, userCreateRequest.password(), profile);
user = userRepository.save(user);
userStatusRepository.save(new UserStatus(user, Instant.now()));   // 명시적 저장

// UserStatus 저장 후 status 필드가 로드된 user를 다시 조회
user = userRepository.findById(user.getId()).orElseThrow();
return userMapper.toDto(user);
```

---

### 2-3. `UserMapper.toDto()` — 강사님 코드의 NPE 위험

**강사님 코드:**
```java
public UserDto toDto(User user) {
    Boolean online = user.getStatus().isOnline();   // ← status가 null이면 NPE 발생
    ...
}
```

**문제점:**
`user.getStatus()`가 `null`을 반환하는 경우(UserStatus 미저장 또는 지연 로딩 미초기화 등)
`.isOnline()` 호출 시 NullPointerException이 발생한다.

**현재 코드 (올바른 방식):**
```java
public UserDto toDto(User user) {
    if (user == null) {
        return null;
    }
    Boolean online = null;
    UserStatus status = user.getStatus();
    if (status != null) {
        online = status.isOnline();
    }
    return new UserDto(user.getId(), user.getUsername(), user.getEmail(), profile, online);
}
```

---

### 2-4. `BasicUserService.update()` — 강사님 코드의 null 중복 체크 문제

**강사님 코드:**
```java
String newUsername = userUpdateRequest.newUsername();
String newEmail = userUpdateRequest.newEmail();
if (userRepository.existsByEmail(newEmail)) {        // ← newEmail이 null이면?
    throw new IllegalArgumentException(...);
}
if (userRepository.existsByUsername(newUsername)) {  // ← newUsername이 null이면?
    throw new IllegalArgumentException(...);
}
```

**문제점:**
`newEmail`, `newUsername`이 null인 경우(변경 안 함)에도 DB에 null 값으로 중복 체크를 한다.
`existsByEmail(null)` 쿼리가 실행되어 예상치 못한 결과를 반환하거나,
null이 email 없는 사용자와 매칭되어 false positiv 예외가 발생할 수 있다.

**현재 코드 (올바른 방식):**
```java
if (newEmail != null && !newEmail.equals(user.getEmail())
    && userRepository.existsByEmail(newEmail)) {
    throw new IllegalArgumentException(...);
}
if (newUsername != null && !newUsername.equals(user.getUsername())
    && userRepository.existsByUsername(newUsername)) {
    throw new IllegalArgumentException(...);
}
```
- null 체크
- 기존 값과 동일하면 중복 검사 스킵 (자기 자신 제외)

---

### 2-5. `LocalBinaryContentStorage.put()` — 강사님 코드의 덮어쓰기 불가 문제

**강사님 코드:**
```java
public UUID put(UUID binaryContentId, byte[] bytes) {
    Path filePath = resolvePath(binaryContentId);
    if (Files.exists(filePath)) {
        throw new IllegalArgumentException("File with key " + binaryContentId + " already exists");
    }
    ...
}
```

**문제점:**
같은 ID의 파일이 이미 존재하면 예외를 던진다.
UUID는 고유하므로 실제로 충돌이 발생할 일은 거의 없지만,
테스트 재실행이나 재시도 시 예외가 발생해 유연성이 낮다.

**현재 코드:**
```java
public UUID put(UUID id, byte[] bytes) {
    Path filePath = resolvePath(id);
    try {
        Files.write(filePath, bytes);   // 존재 시 덮어씀
    } catch (IOException e) {
        throw new RuntimeException("파일 저장 실패: " + id, e);
    }
    return id;
}
```

---

### 2-6. `BinaryContentStorage` 인터페이스 — 강사님 코드에 `delete()` 없음

**강사님 코드:**
```java
public interface BinaryContentStorage {
    UUID put(UUID binaryContentId, byte[] bytes);
    InputStream get(UUID binaryContentId);
    ResponseEntity<?> download(BinaryContentDto metaData);
    // delete() 없음
}
```

**문제점:**
사용자 삭제 또는 프로필 변경 시 기존 파일을 삭제할 방법이 없다.
실제 파일이 누적되어 스토리지가 낭비된다.

**현재 코드:**
```java
public interface BinaryContentStorage {
    UUID put(UUID id, byte[] bytes);
    InputStream get(UUID id);
    ResponseEntity<?> download(BinaryContentDto binaryContentDto);
    void delete(UUID id);   // 파일 삭제 지원
}
```

---

### 2-7. `User.profile` 관계 어노테이션 차이 — 강사님 `@OneToOne` vs 현재 `@ManyToOne`

**강사님 코드:**
```java
@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
@JoinColumn(name = "profile_id", columnDefinition = "uuid")
private BinaryContent profile;
```

**현재 코드:**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "profile_id")
private BinaryContent profile;
```

**차이와 선택 이유:**

| 항목 | 강사님 (`@OneToOne`) | 현재 (`@ManyToOne`) |
|------|----------------------|----------------------|
| DB 제약 | UNIQUE 제약 필요 (없으면 불일치) | FK만 있으면 됨 (DB 스키마와 일치) |
| cascade | ALL + orphanRemoval → profile 자동 삭제 | 없음, 수동 삭제 |
| 파일 삭제 | JPA가 레코드 삭제하지만 파일은 수동 삭제 필요 | `clearProfile()` + `binaryContentStorage.delete()` 로 완전 처리 |

**현재 코드를 유지한 이유:**
DB 스키마에 profile_id에 UNIQUE 제약이 없어 `@OneToOne`과 불일치가 생긴다.
또한 현재 코드는 `clearProfile()` + `flush()` + `deleteById()` + `binaryContentStorage.delete()` 패턴으로
DB 레코드와 실제 파일을 모두 안전하게 삭제하는 완전한 구현을 갖추고 있다.

---

### 2-8. `BaseEntity`의 `@NoArgsConstructor(access = AccessLevel.PROTECTED)` — 불필요

**강사님 코드:**
```java
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity { ... }
```

**이유:**
`abstract` 클래스는 JVM 수준에서 직접 인스턴스화 자체가 불가능하다.
`@NoArgsConstructor(access = AccessLevel.PROTECTED)` 는 상속 클래스에서 `super()` 호출 시
접근 가능한 생성자를 제공하기 위함인데, Lombok이 abstract 클래스에 생성자를 생성해도
컴파일러 관점에서 추가 보호 효과는 없다.
현재 코드는 이를 생략하여 불필요한 어노테이션을 제거했다.

---

## 3. 강사님 코드에서 배운 것 (긍정적 패턴)

| 패턴 | 설명 |
|------|------|
| `@NoArgsConstructor(access = AccessLevel.PROTECTED)` | JPA 기본 생성자를 protected로 제한 → 외부 직접 생성 방지 (현재 코드에 적용 완료) |
| `columnDefinition = "uuid"` / `"timestamp with time zone"` | PostgreSQL 특화 타입 명시 → DDL 자동 생성 시 정확한 타입 지정 가능 |
| `@EnableJpaAuditing` 별도 Config 클래스 분리 | 현재 코드의 `JpaConfig`와 동일 (이미 반영) |

---

## 4. 요약

| 파일 | 강사님 코드 상태 | 현재 코드 상태 | 결정 |
|------|-----------------|---------------|------|
| `BasicUserService.delete()` | 조건 반전 버그 | 정상 | 현재 유지 |
| `BasicUserService.create()` | UserStatus 미저장 버그 | 정상 | 현재 유지 |
| `UserMapper.toDto()` | NPE 위험 | null 안전 처리 | 현재 유지 |
| `BasicUserService.update()` | null 중복 체크 문제 | null 안전 처리 | 현재 유지 |
| `LocalBinaryContentStorage.put()` | 덮어쓰기 불가 | 덮어쓰기 가능 | 현재 유지 |
| `BinaryContentStorage` | `delete()` 없음 | `delete()` 있음 | 현재 유지 |
| `User.profile` | `@OneToOne` + cascade | `@ManyToOne` 수동 삭제 | 현재 유지 (파일까지 완전 삭제) |
| `BaseEntity` `@NoArgsConstructor` | abstract에 불필요하게 추가 | 생략 (abstract이므로 불필요) | 현재 유지 |
| `User/BinaryContent @NoArgsConstructor` | `PROTECTED` | `public` | **강사님 방식 적용** |
