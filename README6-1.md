# 스프린트 미션 6-1 기본 요구사항 구현 정리

> **브랜치: 이종호sm6-1**
> 초보자도 이해할 수 있도록, **왜 이렇게 코딩했는지**를 중심으로 설명합니다.

---

## 목차

1. [전체 프로젝트 구조](#1-전체-프로젝트-구조)
2. [JPA 엔티티 설계](#2-jpa-엔티티-설계)
3. [Spring Data JPA Repository](#3-spring-data-jpa-repository)
4. [DTO와 Mapper 패턴](#4-dto와-mapper-패턴)
5. [Service 레이어](#5-service-레이어)
6. [Controller 레이어](#6-controller-레이어)
7. [파일 저장소 (BinaryContentStorage)](#7-파일-저장소-binarycontentstorage)
8. [예외 처리](#8-예외-처리)
9. [설정 파일](#9-설정-파일)

---

## 1. 전체 프로젝트 구조

### 요청이 처리되는 흐름

```
클라이언트(브라우저/앱)
    ↓ HTTP 요청
Controller         → HTTP 요청을 받고 응답을 돌려주는 문지기
    ↓
Service (interface) → 비즈니스 로직을 담당하는 핵심
    ↓
Repository          → DB와 실제로 대화하는 역할
    ↓
Database (PostgreSQL)
```

**왜 이렇게 나눴나요?**
- 역할을 분리하면 각 부분을 독립적으로 수정할 수 있습니다
- 예: DB를 PostgreSQL에서 MySQL로 바꿔도 Repository만 수정하면 됩니다
- 예: 비즈니스 로직을 바꿔도 Controller는 건드리지 않아도 됩니다

### 레이어별 명명 규칙

| 레이어 | 예시 |
|--------|------|
| 인터페이스(규약) | `UserService`, `ChannelService` |
| 구현체(실제 코드) | `BasicUserService`, `BasicChannelService` |
| Repository | `UserRepository`, `MessageRepository` |
| Controller | `UserController`, `MessageController` |

**왜 Service를 인터페이스와 구현체로 나눴나요?**
- 인터페이스: "이런 기능을 제공합니다"라는 약속(계약)
- 구현체: 실제로 어떻게 동작할지 코드로 작성
- 나중에 구현 방식을 통째로 바꿀 때 유리합니다 (예: `BasicUserService` → `CachedUserService`)

---

## 2. JPA 엔티티 설계

### BaseEntity - 공통 필드를 한 곳에

```java
@MappedSuperclass                    // 이 클래스는 직접 테이블이 되지 않고 상속용으로만 사용
@EntityListeners(AuditingEntityListener.class)  // 자동으로 날짜를 채워주는 리스너
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)  // UUID를 자동 생성
    private UUID id;

    @CreatedDate                       // 저장 시점을 자동으로 기록
    @Column(nullable = false, updatable = false)  // 한 번 설정하면 수정 불가
    private Instant createdAt;
}
```

**왜 이렇게 했나요?**
- `id`와 `createdAt`은 모든 엔티티(User, Channel, Message 등)에 공통으로 필요합니다
- `BaseEntity`에 한 번만 정의하면 모든 엔티티가 상속받아 사용할 수 있습니다
- `@EnableJpaAuditing`(AppConfig.java)이 있어야 `@CreatedDate`가 동작합니다

### User 엔티티

```java
@Entity
@Table(name = "users")
public class User extends BaseUpdatableEntity {

    @Column(length = 50, nullable = false, unique = true)
    private String username;           // 중복 불가

    @Column(length = 100, nullable = false, unique = true)
    private String email;              // 중복 불가

    @ManyToOne(fetch = FetchType.LAZY)  // 프로필 이미지
    @JoinColumn(name = "profile_id")
    private BinaryContent profile;

    @OneToOne(mappedBy = "user",
              cascade = CascadeType.ALL,   // User 삭제 시 UserStatus도 자동 삭제
              orphanRemoval = true,
              fetch = FetchType.EAGER)     // User 조회 시 UserStatus 항상 같이 로드
    private UserStatus status;
}
```

**주요 개념 설명:**

| 어노테이션 | 의미 |
|-----------|------|
| `@ManyToOne(LAZY)` | 여러 User가 하나의 BinaryContent(프로필)를 가질 수 있음. 실제 필요할 때만 DB 조회 |
| `@OneToOne(EAGER)` | User 1명에 UserStatus 1개. User 조회 시 항상 UserStatus도 함께 가져옴 |
| `cascade = ALL` | User를 저장/삭제하면 UserStatus도 자동으로 저장/삭제 |
| `orphanRemoval = true` | User에서 status를 제거하면 UserStatus 데이터도 DB에서 삭제 |

**왜 `status`는 EAGER, `profile`은 LAZY인가요?**
- `status`(온라인 여부)는 User 정보를 표시할 때 거의 항상 필요 → 미리 가져오는 게 효율적
- `profile`(프로필 이미지)은 필요할 때만 가져오면 충분 → 불필요한 쿼리 방지

### Message 엔티티

```java
@Entity
@Table(name = "messages")
public class Message extends BaseUpdatableEntity {

    @Column(columnDefinition = "TEXT")     // 길이 제한 없는 텍스트
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)     // 메시지가 속한 채널
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @ManyToOne(fetch = FetchType.LAZY)     // 메시지 작성자
    @JoinColumn(name = "author_id")        // nullable (탈퇴한 유저의 메시지 유지)
    private User author;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "message_attachments",      // 중간 테이블 이름
        joinColumns = @JoinColumn(name = "message_id"),
        inverseJoinColumns = @JoinColumn(name = "attachment_id")
    )
    private List<BinaryContent> attachments = new ArrayList<>();
}
```

**왜 `@ManyToMany`를 사용했나요?**
- 메시지 1개에 첨부파일 여러 개 가능
- 같은 파일이 여러 메시지에 첨부될 수도 있는 구조
- `message_attachments` 테이블이 중간에서 두 테이블을 연결합니다

**왜 `author_id`는 nullable인가요?**
- 사용자가 탈퇴해도 메시지 기록은 남겨야 합니다
- `ON DELETE SET NULL`: user 삭제 시 `author_id`를 null로 설정

### Channel 엔티티

```java
@Entity
@Table(name = "channels")
public class Channel extends BaseUpdatableEntity {

    @Enumerated(EnumType.STRING)    // DB에 "PUBLIC", "PRIVATE" 문자열로 저장
    private ChannelType type;

    @Column(length = 100)           // nullable: PRIVATE 채널은 이름 없음
    private String name;

    @OneToMany(mappedBy = "channel", fetch = FetchType.LAZY)
    private List<ReadStatus> readStatuses = new ArrayList<>();
}
```

**왜 `@Enumerated(EnumType.STRING)`인가요?**
- `EnumType.ORDINAL`(기본값)은 0, 1 숫자로 저장 → 나중에 순서가 바뀌면 데이터 오류
- `EnumType.STRING`은 "PUBLIC", "PRIVATE" 문자열로 저장 → 가독성도 좋고 안전

---

## 3. Spring Data JPA Repository

### JpaRepository를 상속하면 생기는 것들

```java
public interface UserRepository extends JpaRepository<User, UUID> {

    // 직접 만든 메서드
    Optional<User> findByUsername(String username);  // 이름으로 찾기
    boolean existsByEmail(String email);             // 이메일 존재 여부
    boolean existsByUsername(String username);       // 이름 존재 여부
}
```

`JpaRepository`를 상속하면 아래 기능을 **코드 한 줄 없이** 사용할 수 있습니다:

| 메서드 | 역할 |
|--------|------|
| `save(entity)` | 저장 또는 수정 |
| `findById(id)` | ID로 단건 조회 |
| `findAll()` | 전체 목록 조회 |
| `deleteById(id)` | ID로 삭제 |
| `existsById(id)` | 존재 여부 확인 |

**메서드 이름 규칙 (Spring Data JPA 마법):**
```
findBy + 필드명        → SELECT WHERE 필드 = ?
existsBy + 필드명      → SELECT COUNT WHERE 필드 = ?
findDistinctBy + 조건  → SELECT DISTINCT WHERE 조건

예시:
findByUsername(String username)
  → SELECT * FROM users WHERE username = ?

existsByEmail(String email)
  → SELECT COUNT(*) FROM users WHERE email = ?
```

### 채널 조회 쿼리

```java
// ChannelRepository.java
List<Channel> findDistinctByTypeOrReadStatuses_User_Id(ChannelType type, UUID userId);
```

이 한 줄짜리 메서드가 하는 일:
```sql
SELECT DISTINCT c FROM channels c
LEFT JOIN read_statuses rs ON rs.channel_id = c.id
WHERE c.type = 'PUBLIC'       -- PUBLIC 채널이거나
   OR rs.user_id = {userId}   -- 내가 참여 중인 PRIVATE 채널
```

**왜 `DISTINCT`인가요?**
- PRIVATE 채널에 내가 여러 ReadStatus를 가지면 중복이 생길 수 있어서 제거합니다

### 메시지 쿼리

```java
// MessageRepository.java
Slice<Message> findAllByChannel_Id(UUID channelId, Pageable pageable);  // 페이지네이션

@Query("SELECT MAX(m.createdAt) FROM Message m WHERE m.channel.id = :channelId")
Optional<Instant> findLastCreatedAtByChannelId(@Param("channelId") UUID channelId);
```

**`Slice` vs `Page`:**

| | Slice | Page |
|--|-------|------|
| 포함 정보 | content, hasNext | content, hasNext, totalElements, totalPages |
| COUNT 쿼리 | 없음 | 있음 (전체 개수 조회) |
| 성능 | 빠름 | 느릴 수 있음 |
| 언제? | 무한 스크롤 | 페이지 번호 UI |

채팅 메시지는 전체 개수가 필요 없고 무한 스크롤이므로 `Slice`를 사용합니다.

---

## 4. DTO와 Mapper 패턴

### 왜 DTO가 필요한가요?

```
엔티티(Entity): DB 구조를 그대로 반영한 클래스
DTO(Data Transfer Object): 클라이언트에게 보낼 데이터만 담은 클래스

문제점 - 엔티티를 직접 반환하면:
  - 비밀번호(password) 같은 민감한 정보가 노출될 수 있음
  - DB 구조가 API 응답에 그대로 노출됨
  - 엔티티 변경 시 API가 깨질 수 있음
```

### Mapper - 엔티티 → DTO 변환

```java
// UserMapper.java
@Component
public class UserMapper {

    private final BinaryContentMapper binaryContentMapper;

    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        // 프로필 이미지 변환
        BinaryContentDto profile = binaryContentMapper.toDto(user.getProfile());

        // 온라인 여부 계산 (UserStatus가 없을 수도 있으니 null 체크)
        Boolean online = null;
        UserStatus status = user.getStatus();
        if (status != null) {
            online = status.isOnline();  // 마지막 활동 5분 이내면 true
        }

        return new UserDto(
            user.getId(),
            user.getUsername(),
            user.getEmail(),   // 이메일은 포함
            // password는 포함하지 않음! (보안)
            profile,
            online
        );
    }
}
```

**왜 `@Component`인가요?**
- Spring이 이 클래스를 자동으로 생성해서 관리합니다
- 다른 클래스에서 `@Autowired`나 생성자 주입으로 사용할 수 있습니다

### ChannelMapper - Repository를 직접 사용하는 이유

```java
// ChannelMapper.java
@Component
@RequiredArgsConstructor
public class ChannelMapper {

    private final MessageRepository messageRepository;    // 마지막 메시지 시간 조회용
    private final ReadStatusRepository readStatusRepository; // 참여자 목록 조회용
    private final UserMapper userMapper;

    public ChannelDto toDto(Channel channel) {
        // 이 채널의 마지막 메시지 시간 조회
        Instant lastMessageAt = messageRepository
            .findLastCreatedAtByChannelId(channel.getId())
            .orElse(null);

        // PRIVATE 채널이면 참여자 목록 조회
        List<UserDto> participants = List.of();
        if (channel.getType() == ChannelType.PRIVATE) {
            participants = readStatusRepository
                .findAllByChannel_Id(channel.getId())
                .stream()
                .map(rs -> userMapper.toDto(rs.getUser()))
                .toList();
        }

        return new ChannelDto(channel.getId(), channel.getType(),
                              channel.getName(), channel.getDescription(),
                              participants, lastMessageAt);
    }
}
```

**왜 Mapper에서 Repository를 직접 사용했나요?**
- `ChannelDto`에는 Channel 엔티티에 없는 정보(`lastMessageAt`, `participants`)가 필요합니다
- 이 정보를 가져오려면 DB 조회가 필요하고, Mapper 안에서 처리하는 것이 현재 구조상 자연스럽습니다
- (주의: 이 방식은 N+1 문제를 유발할 수 있습니다 → 심화 요구사항에서 개선)

### Record - 불변 DTO

```java
// UserDto.java
public record UserDto(
    UUID id,
    String username,
    String email,
    BinaryContentDto profile,
    Boolean online
) {}
```

**왜 `record`를 사용했나요?**
- Java 16+에서 추가된 기능으로, **불변(Immutable) 데이터 클래스**를 간결하게 만들 수 있습니다
- 자동으로 생성자, getter, equals, hashCode, toString을 만들어줍니다
- DTO는 데이터 전달용이라 변경될 필요가 없으므로 불변으로 만드는 것이 적합합니다

```java
// record 하나가 아래 코드를 대신합니다
public class UserDto {
    private final UUID id;
    private final String username;
    // ...
    public UserDto(UUID id, String username, ...) { ... }
    public UUID getId() { return id; }
    // equals, hashCode, toString...
}
```

---

## 5. Service 레이어

### @Transactional - 트랜잭션 관리

```java
@Override
@Transactional        // 데이터를 변경하는 메서드 (저장, 수정, 삭제)
public UserDto create(UserCreateRequest request, ...) {
    // 이 메서드 안의 모든 DB 작업이 하나의 단위로 처리됩니다
    // 중간에 오류가 나면 모두 취소(롤백)됩니다
}

@Override
@Transactional(readOnly = true)   // 데이터를 읽기만 하는 메서드
public UserDto find(UUID userId) {
    // readOnly = true: DB에서 읽기만 하고 변경하지 않음을 명시
    // → 성능 최적화 (Hibernate가 변경 감지를 생략)
}
```

**트랜잭션이 필요한 이유:**
```
사용자 생성 시:
  1. User 저장
  2. UserStatus 저장

1번은 성공했는데 2번이 실패하면?
→ @Transactional이 있으면: 1번도 취소되어 데이터 일관성 유지
→ @Transactional이 없으면: User는 저장되었는데 UserStatus는 없는 불완전한 상태
```

### 사용자 생성 - EntityManager.flush() + refresh()

```java
@Transactional
public UserDto create(UserCreateRequest userCreateRequest, ...) {
    // 1. User 저장
    User user = new User(username, email, password, profile);
    user = userRepository.save(user);

    // 2. UserStatus 저장
    userStatusRepository.save(new UserStatus(user, Instant.now()));

    // 3. JPA 1차 캐시 무효화 (중요!)
    entityManager.flush();    // 지금까지의 변경사항을 DB에 반영
    entityManager.refresh(user); // user 객체를 DB에서 다시 로드

    // 4. UserStatus가 포함된 user를 DTO로 변환
    return userMapper.toDto(user);
}
```

**왜 `flush() + refresh()`가 필요한가요?**

```
문제 상황:
  user = userRepository.save(user)
  // 이 시점의 user 객체는 JPA의 1차 캐시(메모리)에 있음
  // user.getStatus()는 아직 null → UserStatus를 방금 저장했지만 연결이 안 됨

  userMapper.toDto(user)
  // user.getStatus()가 null이라 online 필드가 null로 반환됨!

해결:
  entityManager.flush()   → UserStatus 저장을 DB에 즉시 반영
  entityManager.refresh() → user를 DB에서 다시 읽음 → status 포함
```

### 프로필 이미지 교체 로직

```java
@Transactional
public UserDto update(UUID userId, UserUpdateRequest request, Optional<BinaryContentCreateRequest> profileRequest) {
    User user = userRepository.findById(userId).orElseThrow(...);

    BinaryContent newProfile = profileRequest
        .map(req -> {
            // 기존 프로필이 있으면 먼저 삭제
            if (user.getProfile() != null) {
                UUID oldProfileId = user.getProfile().getId();
                user.clearProfile();                      // FK 연결 끊기
                binaryContentRepository.flush();          // FK 제약 조건 해제 먼저
                binaryContentRepository.deleteById(oldProfileId);  // DB 레코드 삭제
                binaryContentStorage.delete(oldProfileId);         // 실제 파일 삭제
            }
            // 새 프로필 저장
            BinaryContent bc = new BinaryContent(...);
            bc = binaryContentRepository.save(bc);
            binaryContentStorage.put(bc.getId(), req.bytes());
            return bc;
        })
        .orElse(null);

    user.update(newUsername, newEmail, newPassword, newProfile);
    return userMapper.toDto(user);
}
```

**왜 `clearProfile()` 후 `flush()`가 필요한가요?**
```
users 테이블의 profile_id가 binary_contents.id를 FK(외래 키)로 참조

삭제 순서:
  1. user.clearProfile()        → users.profile_id = null (연결 끊기)
  2. binaryContentRepository.flush() → DB에 즉시 반영 (profile_id = null)
  3. binaryContentRepository.deleteById() → binary_contents 레코드 삭제 가능

만약 flush() 없이 3번을 먼저 하면?
  → FK 제약 조건 위반 오류 발생!
  (users.profile_id가 아직 oldProfileId를 가리키고 있는데 binary_contents를 지우려 함)
```

### 채널 삭제 - Cascade 활용

```java
@Transactional
public void delete(UUID channelId) {
    Channel channel = channelRepository.findById(channelId).orElseThrow(...);

    // 1. 메시지 첨부파일 목록 미리 수집 (삭제 전에 ID를 알아야 함)
    List<UUID> attachmentIds = messageRepository.findAllByChannel_Id(channelId)
        .stream()
        .flatMap(m -> m.getAttachments().stream())
        .map(BinaryContent::getId)
        .toList();

    // 2. Channel 삭제 → DB cascade로 messages, read_statuses, message_attachments 자동 삭제
    channelRepository.delete(channel);

    // 3. BinaryContent DB 레코드 + 실제 파일 삭제
    attachmentIds.forEach(id -> {
        binaryContentStorage.delete(id);        // 파일 시스템에서 삭제
        binaryContentRepository.deleteById(id); // DB에서 삭제
    });
}
```

**왜 attachmentIds를 미리 수집하나요?**
```
채널을 먼저 삭제하면 (cascade로 메시지도 삭제됨)
→ 어떤 파일들을 지워야 하는지 알 수 없게 됨!

그래서:
  1. 삭제할 파일 ID 목록을 먼저 수집
  2. 채널 삭제 (cascade로 message_attachments도 지워짐)
  3. binary_contents 레코드와 실제 파일 삭제
```

---

## 6. Controller 레이어

### @RestController - REST API 컨트롤러

```java
@RequiredArgsConstructor    // final 필드를 생성자로 주입
@RestController             // HTTP 응답을 JSON으로 자동 변환
@RequestMapping("/api/users")  // 기본 URL 경로
public class UserController implements UserApi {

    private final UserService userService;
    private final UserStatusService userStatusService;
    // Spring이 자동으로 구현체(BasicUserService 등)를 주입해줌
}
```

### HTTP 메서드 매핑

```java
@PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
// POST /api/users  (multipart 형식만 받음 - 파일 업로드 포함)

@PatchMapping(path = "{userId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
// PATCH /api/users/{userId}  (부분 수정)

@DeleteMapping(path = "{userId}")
// DELETE /api/users/{userId}

@GetMapping
// GET /api/users  (전체 목록)
```

**왜 수정에 PUT이 아닌 PATCH를 사용했나요?**
- `PUT`: 리소스 전체를 교체 (모든 필드를 보내야 함)
- `PATCH`: 리소스의 일부만 수정 (변경할 필드만 보내면 됨)
- 사용자 정보 수정 시 username만 바꾸고 싶을 때 PATCH가 더 적합

### Multipart 요청 처리

```java
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<UserDto> create(
    @RequestPart("userCreateRequest") UserCreateRequest userCreateRequest,  // JSON 부분
    @RequestPart(value = "profile", required = false) MultipartFile profile  // 파일 부분
) {
    // profile이 없거나 비어있으면 Optional.empty()
    Optional<BinaryContentCreateRequest> profileRequest = Optional.ofNullable(profile)
        .flatMap(this::resolveProfileRequest);

    UserDto createdUser = userService.create(userCreateRequest, profileRequest);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);  // 201 Created
}
```

**왜 multipart/form-data를 사용했나요?**
```
일반 JSON (application/json):
  { "username": "hong", "email": "hong@test.com" }
  → 파일을 포함할 수 없음

multipart/form-data:
  Part 1 (JSON): { "username": "hong", "email": "hong@test.com" }
  Part 2 (File): [바이너리 파일 데이터]
  → JSON과 파일을 함께 전송 가능
```

### API 인터페이스 분리

```java
// UserApi.java - Swagger 문서용 인터페이스
@Tag(name = "User", description = "User API")
public interface UserApi {
    @Operation(summary = "User 등록")
    @ApiResponses(value = { ... })
    ResponseEntity<UserDto> create(...);
}

// UserController.java - 실제 구현
public class UserController implements UserApi {
    @Override
    public ResponseEntity<UserDto> create(...) {
        // 실제 로직
    }
}
```

**왜 Swagger 어노테이션을 별도 인터페이스에 분리했나요?**
- Swagger 어노테이션은 코드 가독성을 크게 해칩니다
- Controller 파일이 길어지면 핵심 로직을 찾기 어려워집니다
- 분리하면: Controller는 "무엇을"(HTTP 매핑), Interface는 "어떻게 문서화할지"를 담당

### 메시지 페이지네이션

```java
@GetMapping
public ResponseEntity<PageResponse<MessageDto>> findAllByChannelId(
    @RequestParam("channelId") UUID channelId,
    Pageable pageable  // Spring이 쿼리 파라미터를 자동으로 Pageable로 변환
) {
    // 기본값 설정: 페이지 0, 최대 50개, createdAt 내림차순 정렬
    Pageable defaultPageable = PageRequest.of(
        pageable.getPageNumber(),
        pageable.getPageSize() > 0 ? pageable.getPageSize() : 50,
        pageable.getSortOr(Sort.by(Sort.Direction.DESC, "createdAt"))
    );

    PageResponse<MessageDto> response = messageService.findAllByChannelId(channelId, defaultPageable);
    return ResponseEntity.ok(response);
}
```

**`Pageable` 동작 방식:**
```
GET /api/messages?channelId=xxx&page=0&size=50&sort=createdAt,desc

Spring이 자동으로:
  page=0      → 첫 번째 페이지
  size=50     → 한 페이지에 50개
  sort=createdAt,desc → createdAt 기준 내림차순
을 Pageable 객체로 변환해줍니다
```

---

## 7. 파일 저장소 (BinaryContentStorage)

### 인터페이스와 구현체 분리

```java
// BinaryContentStorage.java - 인터페이스 (규약)
public interface BinaryContentStorage {
    UUID put(UUID id, byte[] bytes);    // 파일 저장
    InputStream get(UUID id);           // 파일 읽기
    ResponseEntity<Resource> download(BinaryContentDto metaData);  // 다운로드 응답
    void delete(UUID id);               // 파일 삭제
}

// LocalBinaryContentStorage.java - 로컬 파일 시스템 구현체
@Component
@ConditionalOnProperty(name = "discodeit.storage.type", havingValue = "local")
public class LocalBinaryContentStorage implements BinaryContentStorage {
    // ...
}
```

**왜 인터페이스로 분리했나요?**
- 나중에 로컬 저장소 → AWS S3, Google Cloud Storage 등으로 바꿀 때
- `LocalBinaryContentStorage` 대신 `S3BinaryContentStorage`를 만들기만 하면 됩니다
- 서비스 코드는 전혀 건드리지 않아도 됩니다

### @ConditionalOnProperty - 조건부 빈 등록

```yaml
# application.yaml
discodeit:
  storage:
    type: local           ← 이 값에 따라 어떤 구현체를 사용할지 결정
    local:
      root-path: .discodeit-storage
```

```java
@ConditionalOnProperty(name = "discodeit.storage.type", havingValue = "local")
public class LocalBinaryContentStorage implements BinaryContentStorage {
    // type=local 일 때만 이 Bean을 Spring에 등록
}
```

### @PostConstruct - 초기화

```java
@PostConstruct  // Spring Bean이 생성된 직후 자동으로 실행
public void init() {
    if (Files.notExists(root)) {
        Files.createDirectories(root);  // 저장 디렉토리가 없으면 생성
    }
}
```

### 파일 저장 구조

```
.discodeit-storage/
├── 550e8400-e29b-41d4-a716-446655440000  ← UUID 파일명 (확장자 없음)
├── 6ba7b810-9dad-11d1-80b4-00c04fd430c8
└── 7c9f3e4a-b2d1-4e8f-9c3d-5f2a1b8e7d6c
```

**왜 UUID를 파일명으로 사용했나요?**
- 같은 이름의 파일을 여러 사용자가 올려도 충돌이 없습니다
- DB의 `binary_contents.id`와 파일명이 일치하므로 추적이 쉽습니다

---

## 8. 예외 처리

### GlobalExceptionHandler - 전역 예외 처리

```java
@RestControllerAdvice  // 모든 Controller에 적용되는 예외 처리기
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleException(IllegalArgumentException e) {
        e.printStackTrace();  // 서버 로그에 오류 출력
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)   // 400 반환
            .body(e.getMessage());             // 오류 메시지를 body에 담아 반환
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleException(NoSuchElementException e) {
        e.printStackTrace();
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)     // 404 반환
            .body(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        e.printStackTrace();
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)  // 500 반환
            .body(e.getMessage());
    }
}
```

**예외와 HTTP 상태코드 매핑:**

| 예외 | 상황 | HTTP 코드 |
|------|------|-----------|
| `IllegalArgumentException` | 이미 존재하는 이메일로 가입, Private 채널 수정 시도 등 | 400 Bad Request |
| `NoSuchElementException` | 존재하지 않는 User/Channel/Message ID 조회 | 404 Not Found |
| `Exception` | 예상치 못한 서버 오류 | 500 Internal Server Error |

**왜 Service에서 예외를 던지나요?**

```java
// Service에서
return userRepository.findById(userId)
    .orElseThrow(() -> new NoSuchElementException("User with id " + userId + " not found"));
//   ↑ 없으면 예외 던지기

// GlobalExceptionHandler가 받아서
// → HTTP 404 응답으로 변환
```

- Controller에서 일일이 `if (user == null) return 404;` 하지 않아도 됩니다
- 한 곳(GlobalExceptionHandler)에서 예외 처리를 모아 관리합니다

---

## 9. 설정 파일

### application.yaml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/discodeit
    username: discodeit_user
    password: discodeit1234

  jpa:
    hibernate:
      ddl-auto: validate    # 스키마를 자동으로 생성하지 않음. schema.sql로 수동 생성
    show-sql: true           # 실행되는 SQL을 콘솔에 출력 (개발 시 디버깅용)
    properties:
      hibernate:
        format_sql: true     # SQL을 들여쓰기 형식으로 보기 좋게 출력
        use_sql_comments: true  # SQL에 어느 쿼리인지 주석 추가

  sql:
    init:
      mode: never            # schema.sql을 자동 실행하지 않음 (수동으로 적용)

discodeit:
  storage:
    type: local              # 파일 저장소 타입 (local만 지원)
    local:
      root-path: .discodeit-storage  # 파일 저장 경로
```

**왜 `ddl-auto: validate`인가요?**

| 옵션 | 의미 |
|------|------|
| `create` | 매번 테이블을 새로 만듦 → **데이터 초기화** (개발 초기에만) |
| `update` | 스키마 변경 사항을 자동 적용 |
| `validate` | 엔티티와 DB 스키마가 일치하는지 검증만 함 → **데이터 보존** |
| `none` | 아무것도 안 함 |

- `validate`를 써야 서버를 재시작해도 기존 데이터가 유지됩니다
- `schema.sql`을 직접 작성해서 테이블을 만들고, 이후에는 validate로 검증만 합니다

---

## 전체 흐름 요약

### 메시지 전송 예시

```
1. 클라이언트
   POST /api/messages (multipart: messageCreateRequest JSON + attachments 파일들)

2. MessageController.create()
   - MultipartFile[] → BinaryContentCreateRequest[] 변환
   - messageService.create() 호출

3. BasicMessageService.create()  [@Transactional]
   - Channel 존재 확인 (없으면 404)
   - User(author) 존재 확인 (없으면 404)
   - 첨부파일마다:
     * BinaryContent 엔티티 생성 → DB 저장
     * 실제 파일 → .discodeit-storage/{UUID} 저장
   - Message 엔티티 생성 → DB 저장
   - messageMapper.toDto(message) → MessageDto 반환

4. MessageController
   - 201 Created + MessageDto(JSON) 응답
```

### 핵심 개념 한 줄 요약

| 개념 | 한 줄 요약 |
|------|-----------|
| **JPA Entity** | DB 테이블을 Java 클래스로 표현 |
| **Spring Data JPA** | 메서드 이름만으로 SQL 쿼리를 자동 생성 |
| **DTO** | 클라이언트에게 보낼 데이터만 골라 담은 객체 |
| **Mapper** | Entity → DTO 변환을 담당하는 클래스 |
| **@Transactional** | DB 작업을 하나의 단위로 묶어 오류 시 전체 취소 |
| **@RestControllerAdvice** | 모든 컨트롤러의 예외를 한 곳에서 처리 |
| **Slice** | 전체 개수 없이 다음 페이지 존재 여부만 제공하는 페이지네이션 |
| **@ConditionalOnProperty** | 설정 값에 따라 Bean을 조건부로 등록 |
