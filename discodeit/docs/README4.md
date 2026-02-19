# 📚 디스코드잇 프로젝트 완벽 가이드 (초보자용)

> 웹 API 기반 Spring Boot 애플리케이션의 전체 구조와 동작 원리를 이해하기 위한 완벽 가이드

---

## 📖 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [전체 아키텍처](#2-전체-아키텍처)
3. [계층별 상세 설명](#3-계층별-상세-설명)
4. [웹 API 동작 원리](#4-웹-api-동작-원리)
5. [주요 기능 구현](#5-주요-기능-구현)
6. [데이터 흐름 예시](#6-데이터-흐름-예시)
7. [초보자를 위한 핵심 개념](#7-초보자를-위한-핵심-개념)
8. [실습 예제](#8-실습-예제)

---

## 1. 프로젝트 개요

### 1.1 디스코드잇이란?

**디스코드잇(DiscoDeIt)**은 Discord와 유사한 메시징 플랫폼을 학습 목적으로 구현한 프로젝트입니다.

**주요 기능:**
- 👤 **사용자 관리**: 회원가입, 로그인, 프로필 관리
- 💬 **채널 관리**: 공개/비공개 채널 생성 및 관리
- 📨 **메시지 관리**: 메시지 전송, 수정, 삭제
- 📎 **파일 관리**: 프로필 이미지, 첨부파일 업로드
- 👀 **읽기 상태**: 메시지 읽음/안읽음 추적
- 🟢 **온라인 상태**: 사용자 온라인/오프라인 표시

### 1.2 기술 스택

| 분류 | 기술 |
|------|------|
| **프레임워크** | Spring Boot 3.x |
| **언어** | Java 17+ |
| **빌드 도구** | Gradle |
| **데이터 저장** | Java Collection Framework (HashMap) - 메모리 기반 |
| **API 스타일** | REST API |
| **직렬화** | Jackson (JSON) |

### 1.3 왜 메모리 기반인가?

**학습 목적:**
- JPA나 데이터베이스 설정 없이 빠르게 시작 가능
- 데이터 저장 로직을 직접 구현하면서 Repository 패턴 이해
- 나중에 JPA로 쉽게 교체 가능한 구조

**실무에서는:**
- 실제 프로젝트에서는 MySQL, PostgreSQL 등의 데이터베이스 사용
- Spring Data JPA로 Repository 자동 생성
- 트랜잭션 관리 및 데이터 영속성 보장

---

## 2. 전체 아키텍처

### 2.1 계층형 아키텍처 (Layered Architecture)

```
┌─────────────────────────────────────────────────────────────┐
│                   Client (Postman, 브라우저)                │
│                      HTTP 요청/응답                         │
└────────────────────────┬────────────────────────────────────┘
                         │ JSON
                         ↓
┌─────────────────────────────────────────────────────────────┐
│              🎯 Controller Layer (REST API)                 │
│   - UserController, ChannelController, MessageController    │
│   - HTTP 요청을 받아서 적절한 Service 메서드 호출          │
│   - Service 결과를 HTTP 응답으로 변환                      │
└────────────────────────┬────────────────────────────────────┘
                         │ DTO (Request, Response)
                         ↓
┌─────────────────────────────────────────────────────────────┐
│                 💼 Service Layer (비즈니스 로직)            │
│        - BasicUserService, BasicChannelService...           │
│        - 비즈니스 규칙 검증 (중복 체크, 권한 확인)         │
│        - 여러 Repository 조합하여 복잡한 로직 구현         │
│        - Entity ↔ DTO 변환                                  │
└────────────────────────┬────────────────────────────────────┘
                         │ Entity
                         ↓
┌─────────────────────────────────────────────────────────────┐
│              💾 Repository Layer (데이터 접근)              │
│     - JCFUserRepository, JCFChannelRepository...            │
│     - HashMap을 사용한 CRUD 연산                           │
│     - 데이터 검색 및 필터링                                │
└────────────────────────┬────────────────────────────────────┘
                         │ HashMap (메모리)
                         ↓
┌─────────────────────────────────────────────────────────────┐
│                🗂️ Entity Layer (도메인 모델)                │
│        - User, Channel, Message, ReadStatus...              │
│        - 비즈니스 데이터 구조 정의                         │
│        - 엔티티 내부 로직 (update, isOnline 등)            │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 각 계층의 역할

| 계층 | 역할 | 예시 |
|------|------|------|
| **Controller** | HTTP 요청 처리 및 응답 반환 | `POST /users` → `createUser()` |
| **Service** | 비즈니스 로직 실행 | 중복 체크, 데이터 변환, 연관 데이터 처리 |
| **Repository** | 데이터 저장/조회 | `save()`, `findById()`, `delete()` |
| **Entity** | 데이터 모델 정의 | `User`, `Channel`, `Message` |

### 2.3 의존성 방향

```
Controller → Service → Repository → Entity

각 계층은 바로 아래 계층만 의존
Controller는 Repository를 직접 사용하지 않음
Service는 여러 Repository를 조합 가능
```

---

## 3. 계층별 상세 설명

### 3.1 Entity Layer (도메인 모델)

**위치:** `src/main/java/com/sprint/mission/discodeit/entity/`

#### 3.1.1 BaseEntity

모든 엔티티의 공통 속성을 정의합니다.

```java
public class BaseEntity {
    private UUID id;              // 고유 식별자
    private Instant createdAt;    // 생성 시간
    private Instant updatedAt;    // 수정 시간

    // 수정 시간 자동 갱신
    protected void updateTimeStamp() {
        this.updatedAt = Instant.now();
    }
}
```

**왜 BaseEntity가 필요한가?**
- 모든 테이블에 공통으로 필요한 필드 중복 제거
- 생성/수정 시간 자동 관리
- 코드 재사용성 향상

#### 3.1.2 User (사용자)

```java
@Getter
public class User extends BaseEntity {
    private String username;    // 사용자명 (로그인 ID)
    private String email;       // 이메일
    private String password;    // 비밀번호 (실무: 암호화 필요)
    private UUID profileId;     // 프로필 이미지 ID (BinaryContent 참조)

    public void update(String username, String email,
                       String password, UUID profileId) {
        // null이 아닌 값만 업데이트
        if (username != null) this.username = username;
        if (email != null) this.email = email;
        // ...
        updateTimeStamp(); // 수정 시간 갱신
    }
}
```

**핵심 포인트:**
- `@Getter`: Lombok으로 getter 메서드 자동 생성
- `profileId`: 프로필 이미지를 직접 저장하지 않고 ID만 참조
- `update()`: 부분 업데이트 지원 (null 값은 유지)

#### 3.1.3 Channel (채널)

```java
@Getter
public class Channel extends BaseEntity {
    private ChannelType type;    // PUBLIC 또는 PRIVATE
    private String name;         // 채널 이름
    private String description;  // 채널 설명

    // PUBLIC 채널 생성자
    public Channel(String name, String description) {
        super();
        this.type = ChannelType.PUBLIC;
        this.name = name;
        this.description = description;
    }

    // PRIVATE 채널 생성자
    public Channel() {
        super();
        this.type = ChannelType.PRIVATE;
    }
}
```

**채널 타입 차이:**
- **PUBLIC**: 모든 사용자가 접근 가능, 이름과 설명이 있음
- **PRIVATE**: 초대된 사용자만 접근, 이름 없음 (1:1 또는 그룹 DM)

#### 3.1.4 Message (메시지)

```java
@Getter
public class Message extends BaseEntity {
    private String content;           // 메시지 내용
    private UUID channelId;           // 어느 채널의 메시지인지
    private UUID authorId;            // 누가 작성했는지
    private List<UUID> attachmentIds; // 첨부파일 ID 목록

    public void update(String content) {
        if (content != null) {
            this.content = content;
            updateTimeStamp();
        }
    }
}
```

**관계:**
- `channelId` → Channel 참조
- `authorId` → User 참조
- `attachmentIds` → BinaryContent 목록 참조

#### 3.1.5 UserStatus (사용자 상태)

```java
@Getter
public class UserStatus extends BaseEntity {
    private UUID userId;              // 사용자 ID
    private Instant lastActiveAt;     // 마지막 활동 시간

    // 5분 이내 활동했으면 온라인
    public boolean isOnline() {
        return Duration.between(lastActiveAt, Instant.now())
                       .toMinutes() < 5;
    }
}
```

**온라인 판단 로직:**
- 마지막 활동 시간이 현재로부터 5분 이내 → 온라인
- 5분 이상 경과 → 오프라인

#### 3.1.6 ReadStatus (읽기 상태)

```java
@Getter
public class ReadStatus extends BaseEntity {
    private UUID userId;           // 사용자 ID
    private UUID channelId;        // 채널 ID
    private Instant lastReadAt;    // 마지막으로 읽은 시간
}
```

**두 가지 역할:**
1. **PRIVATE 채널 접근 제어**: ReadStatus가 있는 사용자만 채널 볼 수 있음
2. **메시지 읽음 추적**: 언제 마지막으로 메시지를 읽었는지 기록

---

### 3.2 Repository Layer (데이터 접근)

**위치:** `src/main/java/com/sprint/mission/discodeit/repository/jcf/`

#### 3.2.1 Repository 인터페이스

```java
public interface UserRepository {
    User save(User user);                     // 저장
    Optional<User> findById(UUID id);         // ID로 조회
    Optional<User> findByUsername(String username); // username으로 조회
    List<User> findAll();                     // 전체 조회
    void deleteById(UUID id);                 // 삭제
    boolean existsByUsername(String username); // 중복 체크
}
```

**왜 인터페이스를 사용하는가?**
- JCF 구현체를 나중에 JPA 구현체로 쉽게 교체 가능
- 테스트 시 Mock 객체 사용 가능
- 코드 의존성을 인터페이스에만 두어 결합도 낮춤

#### 3.2.2 JCF 구현체

```java
@Repository
@ConditionalOnProperty(
    name = "discodeit.repository.type",
    havingValue = "jcf",
    matchIfMissing = true
)
public class JCFUserRepository implements UserRepository {
    // HashMap으로 메모리에 데이터 저장
    private final Map<UUID, User> data = new HashMap<>();

    @Override
    public User save(User user) {
        data.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(data.get(id));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return data.values().stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst();
    }

    // ... 기타 메서드
}
```

**핵심 포인트:**
- `@ConditionalOnProperty`: 설정에 따라 이 구현체 활성화
- `HashMap<UUID, User>`: ID를 키로 사용하여 빠른 조회
- `Stream API`: 복잡한 검색 조건 처리 (username, email 등)

**장단점:**

| 장점 | 단점 |
|------|------|
| ✅ 설정 없이 바로 사용 가능 | ❌ 서버 재시작 시 데이터 손실 |
| ✅ 빠른 CRUD 연산 (메모리) | ❌ 동시성 문제 (HashMap은 thread-safe 하지 않음) |
| ✅ 학습용으로 이해하기 쉬움 | ❌ 메모리 용량 제한 |

---

### 3.3 Service Layer (비즈니스 로직)

**위치:** `src/main/java/com/sprint/mission/discodeit/service/basic/`

#### 3.3.1 BasicUserService

```java
@Service
@RequiredArgsConstructor
public class BasicUserService implements UserService {
    private final UserRepository userRepository;
    private final BinaryContentRepository binaryContentRepository;
    private final UserStatusRepository userStatusRepository;

    @Override
    public UserResponse create(UserCreateRequest request,
                               BinaryContentCreateRequest profileRequest) {
        // 1. 비즈니스 규칙 검증
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException(
                "이 사용자 이름은 이미 존재해요!: " + request.username()
            );
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException(
                "이 이메일은 이미 존재해요!: " + request.email()
            );
        }

        // 2. 프로필 이미지 저장 (있는 경우)
        UUID profileId = null;
        if (profileRequest != null) {
            BinaryContent profile = new BinaryContent(
                profileRequest.fileName(),
                profileRequest.contentType(),
                profileRequest.data()
            );
            profileId = binaryContentRepository.save(profile).getId();
        }

        // 3. User 엔티티 생성 및 저장
        User user = new User(
            request.username(),
            request.email(),
            request.password(),
            profileId
        );
        User savedUser = userRepository.save(user);

        // 4. UserStatus 생성 (초기값: 현재 시간)
        UserStatus userStatus = new UserStatus(
            savedUser.getId(),
            Instant.now()
        );
        userStatusRepository.save(userStatus);

        // 5. Entity를 DTO로 변환하여 반환
        return toUserResponse(savedUser, true);
    }

    private UserResponse toUserResponse(User user, boolean isOnline) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getProfileId(),
            isOnline,  // 비밀번호는 제외!
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
```

**Service Layer의 핵심 역할:**

1. **비즈니스 규칙 검증**
   - 중복 체크 (username, email)
   - 권한 확인
   - 데이터 유효성 검사

2. **여러 Repository 조합**
   - User + BinaryContent + UserStatus를 함께 관리
   - 트랜잭션 단위 작업 (실무에서는 `@Transactional`)

3. **Entity ↔ DTO 변환**
   - 비밀번호 같은 민감 정보 제외
   - 추가 정보 계산 (isOnline)

#### 3.3.2 BasicChannelService

```java
@Service
@RequiredArgsConstructor
public class BasicChannelService implements ChannelService {
    private final ChannelRepository channelRepository;
    private final ReadStatusRepository readStatusRepository;
    private final MessageRepository messageRepository;
    private final BinaryContentRepository binaryContentRepository;

    @Override
    public ChannelResponse createPrivate(PrivateChannelCreateRequest request) {
        // 1. PRIVATE 채널 생성
        Channel channel = new Channel(); // 이름 없는 PRIVATE 채널
        Channel savedChannel = channelRepository.save(channel);

        // 2. 각 참여자에게 ReadStatus 생성 (접근 권한 부여)
        for (UUID memberId : request.memberIds()) {
            ReadStatus readStatus = new ReadStatus(
                memberId,
                savedChannel.getId(),
                Instant.now()
            );
            readStatusRepository.save(readStatus);
        }

        return toChannelResponse(savedChannel);
    }

    @Override
    public void delete(UUID id) {
        // 연쇄 삭제 (Cascade Delete)
        Channel channel = channelRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                    "Channel not found: " + id
                ));

        // 1. 채널의 메시지 조회
        List<Message> messages = messageRepository.findAllByChannelId(id);

        // 2. 각 메시지의 첨부파일 삭제
        for (Message message : messages) {
            for (UUID attachmentId : message.getAttachmentIds()) {
                binaryContentRepository.deleteById(attachmentId);
            }
        }

        // 3. 메시지 일괄 삭제
        messageRepository.deleteAllByChannelId(id);

        // 4. ReadStatus 일괄 삭제
        readStatusRepository.deleteAllByChannelId(id);

        // 5. 최종적으로 채널 삭제
        channelRepository.deleteById(id);
    }
}
```

**PRIVATE 채널의 접근 제어:**

```java
@Override
public List<ChannelResponse> findAllByUserId(UUID userId) {
    List<Channel> allChannels = channelRepository.findAll();

    return allChannels.stream()
            .filter(channel -> {
                if (channel.getType() == ChannelType.PUBLIC) {
                    return true; // PUBLIC은 모두 접근 가능
                }
                // PRIVATE은 ReadStatus가 있어야 접근 가능
                return readStatusRepository
                        .findByUserIdAndChannelId(userId, channel.getId())
                        .isPresent();
            })
            .map(this::toChannelResponse)
            .toList();
}
```

**연쇄 삭제 순서가 중요한 이유:**
1. 첨부파일 먼저 삭제 (메시지가 참조하기 때문)
2. 메시지 삭제 (채널이 참조하기 때문)
3. ReadStatus 삭제 (채널을 참조하기 때문)
4. 마지막에 채널 삭제

---

### 3.4 Controller Layer (REST API)

**위치:** `src/main/java/com/sprint/mission/discodeit/controller/`

#### 3.4.1 UserController

```java
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserStatusService userStatusService;

    /**
     * 사용자 등록
     * POST /users
     *
     * 요청 본문:
     * {
     *   "username": "user1",
     *   "email": "user1@example.com",
     *   "password": "password123"
     * }
     *
     * 응답: 201 Created + UserResponse
     */
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<UserResponse> createUser(
            @RequestBody UserCreateRequest userRequest
    ) {
        UserResponse user = userService.create(userRequest, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * 전체 사용자 조회
     * GET /users
     *
     * 응답: 200 OK + List<UserResponse>
     */
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.findAll();
        return ResponseEntity.ok(users);
    }

    /**
     * 사용자 온라인 상태 업데이트
     * PUT /users/{id}/status
     *
     * 요청 본문:
     * {
     *   "lastActiveAt": "2024-01-15T10:30:00Z"
     * }
     *
     * 응답: 200 OK + UserStatus
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/{id}/status")
    public ResponseEntity<UserStatus> updateUserStatus(
            @PathVariable UUID id,
            @RequestBody UserStatusUpdateRequest request
    ) {
        UserStatus userStatus = userStatusService.updateByUserId(id, request);
        return ResponseEntity.ok(userStatus);
    }
}
```

**Spring MVC 주요 어노테이션:**

| 어노테이션 | 역할 | 예시 |
|-----------|------|------|
| `@RestController` | REST API 컨트롤러임을 표시 | JSON 응답 자동 변환 |
| `@RequestMapping("/users")` | 기본 URL 경로 | 모든 메서드에 `/users` 접두사 |
| `@RequestBody` | HTTP 요청 본문을 Java 객체로 변환 | JSON → UserCreateRequest |
| `@PathVariable` | URL 경로의 변수 추출 | `/users/{id}` → `UUID id` |
| `@RequestParam` | 쿼리 파라미터 추출 | `?userId=xxx` → `UUID userId` |

**HTTP 상태 코드:**

| 상태 코드 | 의미 | 사용 시점 |
|----------|------|----------|
| **200 OK** | 성공 | 조회, 수정 성공 |
| **201 Created** | 생성 성공 | 사용자 등록, 채널 생성 |
| **204 No Content** | 성공 (응답 본문 없음) | 삭제 성공 |
| **404 Not Found** | 리소스 없음 | 존재하지 않는 ID 조회 |
| **400 Bad Request** | 잘못된 요청 | 유효성 검사 실패 |

#### 3.4.2 ChannelController

```java
@RestController
@RequestMapping("/channels")
@RequiredArgsConstructor
public class ChannelController {
    private final ChannelService channelService;

    /**
     * 공개 채널 생성
     * POST /channels/public
     */
    @RequestMapping(method = RequestMethod.POST, value = "/public")
    public ResponseEntity<ChannelResponse> createPublicChannel(
            @RequestBody PublicChannelCreateRequest request
    ) {
        ChannelResponse channel = channelService.createPublic(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(channel);
    }

    /**
     * 비공개 채널 생성
     * POST /channels/private
     */
    @RequestMapping(method = RequestMethod.POST, value = "/private")
    public ResponseEntity<ChannelResponse> createPrivateChannel(
            @RequestBody PrivateChannelCreateRequest request
    ) {
        ChannelResponse channel = channelService.createPrivate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(channel);
    }

    /**
     * 사용자가 볼 수 있는 채널 조회
     * GET /channels?userId={userId}
     */
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<ChannelResponse>> getChannelsByUser(
            @RequestParam UUID userId
    ) {
        List<ChannelResponse> channels = channelService.findAllByUserId(userId);
        return ResponseEntity.ok(channels);
    }
}
```

---

### 3.5 DTO Layer (데이터 전송 객체)

**위치:** `src/main/java/com/sprint/mission/discodeit/dto/`

#### 3.5.1 Request DTO

```java
// 사용자 생성 요청
public record UserCreateRequest(
    String username,
    String email,
    String password
) {}

// 로그인 요청
public record LoginRequest(
    String username,
    String password
) {}

// 메시지 전송 요청
public record MessageCreateRequest(
    String content,
    UUID channelId,
    UUID authorId
) {}
```

**Java Record의 장점:**
- 불변 객체 (Immutable)
- getter 자동 생성 (`request.username()`)
- `equals()`, `hashCode()`, `toString()` 자동 생성
- 간결한 코드

#### 3.5.2 Response DTO

```java
// 사용자 응답
public record UserResponse(
    UUID id,
    String username,
    String email,
    UUID profileId,
    boolean isOnline,    // 계산된 값
    Instant createdAt,
    Instant updatedAt
    // password는 포함하지 않음!
) {}

// 채널 응답
public record ChannelResponse(
    UUID id,
    ChannelType type,
    String name,
    String description,
    List<UUID> participantIds,
    Instant lastMessageAt,
    Instant createdAt,
    Instant updatedAt
) {}
```

**Entity vs DTO 차이:**

| 구분 | Entity | DTO |
|------|--------|-----|
| **목적** | 데이터베이스 모델 | 네트워크 전송 |
| **변경** | 가변 (Mutable) | 불변 (Immutable) |
| **정보** | 모든 정보 포함 | 필요한 정보만 |
| **민감 정보** | 포함 (password) | 제외 |
| **추가 정보** | 없음 | 계산된 값 포함 (isOnline) |

---

## 4. 웹 API 동작 원리

### 4.1 HTTP 요청부터 응답까지의 흐름

```
┌─────────────────────────────────────────────────────────────┐
│ 1️⃣ HTTP 요청                                                │
│    POST http://localhost:8080/users                         │
│    Content-Type: application/json                           │
│    Body: {"username":"user1","email":"...","password":"..."} │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ↓ Jackson이 JSON을 Java 객체로 변환
┌─────────────────────────────────────────────────────────────┐
│ 2️⃣ Spring MVC (DispatcherServlet)                           │
│    - URL 매핑 확인: /users → UserController                 │
│    - HTTP 메서드 확인: POST → createUser()                  │
│    - @RequestBody로 UserCreateRequest 객체 생성             │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ↓
┌─────────────────────────────────────────────────────────────┐
│ 3️⃣ UserController.createUser()                              │
│    @RequestMapping(method = RequestMethod.POST)             │
│    public ResponseEntity<UserResponse> createUser(          │
│        @RequestBody UserCreateRequest request               │
│    ) {                                                      │
│        UserResponse user = userService.create(request, null);│
│        return ResponseEntity.status(201).body(user);        │
│    }                                                        │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ↓ Service 호출
┌─────────────────────────────────────────────────────────────┐
│ 4️⃣ BasicUserService.create()                                │
│    1. 중복 체크 (userRepository.existsByUsername)           │
│    2. 프로필 이미지 저장 (binaryContentRepository.save)     │
│    3. User 엔티티 생성 및 저장 (userRepository.save)        │
│    4. UserStatus 생성 및 저장 (userStatusRepository.save)   │
│    5. UserResponse DTO로 변환하여 반환                      │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ↓ Repository 호출
┌─────────────────────────────────────────────────────────────┐
│ 5️⃣ JCFUserRepository.save()                                 │
│    private Map<UUID, User> data = new HashMap<>();          │
│                                                             │
│    public User save(User user) {                           │
│        data.put(user.getId(), user);                       │
│        return user;                                        │
│    }                                                        │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ↓ 데이터 저장 완료
┌─────────────────────────────────────────────────────────────┐
│ 6️⃣ UserResponse 반환                                        │
│    Service → Controller로 UserResponse 반환                 │
│    {                                                        │
│      "id": "uuid-value",                                   │
│      "username": "user1",                                  │
│      "email": "user1@example.com",                         │
│      "isOnline": true,                                     │
│      "createdAt": "2024-01-15T10:30:00Z"                   │
│    }                                                        │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ↓ Jackson이 Java 객체를 JSON으로 변환
┌─────────────────────────────────────────────────────────────┐
│ 7️⃣ HTTP 응답                                                │
│    HTTP/1.1 201 Created                                    │
│    Content-Type: application/json                          │
│    Body: {"id":"...","username":"user1",...}               │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 Spring Boot의 자동 처리 기능

**Spring Boot가 자동으로 해주는 일:**

1. **JSON ↔ Java 객체 변환 (Jackson)**
   - 요청: JSON → `UserCreateRequest`
   - 응답: `UserResponse` → JSON

2. **의존성 주입 (Dependency Injection)**
   - `@RequiredArgsConstructor`로 생성자 자동 생성
   - Controller에 Service 자동 주입
   - Service에 Repository 자동 주입

3. **HTTP 상태 코드 설정**
   - `ResponseEntity.status(HttpStatus.CREATED)` → `201 Created`
   - `ResponseEntity.ok()` → `200 OK`

4. **예외 처리 (GlobalExceptionHandler)**
   - `NoSuchElementException` → `404 Not Found`
   - `IllegalArgumentException` → `400 Bad Request`

---

## 5. 주요 기능 구현

### 5.1 사용자 인증 (로그인)

```java
@Service
@RequiredArgsConstructor
public class BasicAuthService implements AuthService {
    private final UserRepository userRepository;
    private final UserStatusRepository userStatusRepository;

    @Override
    public UserResponse login(LoginRequest request) {
        // 1. username으로 사용자 검색
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new NoSuchElementException(
                    "User not found with username: " + request.username()
                ));

        // 2. 비밀번호 검증 (실무에서는 암호화된 비밀번호 비교)
        if (!user.getPassword().equals(request.password())) {
            throw new IllegalArgumentException("Invalid password");
        }

        // 3. 온라인 상태 조회
        boolean isOnline = userStatusRepository.findByUserId(user.getId())
                .map(UserStatus::isOnline)
                .orElse(false);

        // 4. UserResponse 반환
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
```

**실무와의 차이:**
- **현재**: 평문 비밀번호 비교 (`password.equals()`)
- **실무**: BCrypt, Argon2 등으로 암호화된 비밀번호 비교
- **실무**: JWT 토큰 발급, 세션 관리

### 5.2 PRIVATE 채널 생성

```java
@Override
public ChannelResponse createPrivate(PrivateChannelCreateRequest request) {
    // 1. PRIVATE 채널 생성 (이름 없음)
    Channel channel = new Channel(); // type = PRIVATE
    Channel savedChannel = channelRepository.save(channel);

    // 2. 각 참여자에게 ReadStatus 생성 (접근 권한 부여)
    for (UUID memberId : request.memberIds()) {
        ReadStatus readStatus = new ReadStatus(
            memberId,
            savedChannel.getId(),
            Instant.now()
        );
        readStatusRepository.save(readStatus);
    }

    // 3. ChannelResponse 반환
    return new ChannelResponse(
        savedChannel.getId(),
        savedChannel.getType(),
        null, // PRIVATE 채널은 이름 없음
        null, // 설명도 없음
        request.memberIds(), // 참여자 목록
        null,
        savedChannel.getCreatedAt(),
        savedChannel.getUpdatedAt()
    );
}
```

**ReadStatus의 역할:**
- PRIVATE 채널 생성 시 각 참여자에게 ReadStatus 생성
- 나중에 채널 조회 시 ReadStatus로 접근 권한 확인

### 5.3 메시지 전송 및 첨부파일

```java
@Override
public MessageResponse create(MessageCreateRequest request,
                               List<BinaryContentCreateRequest> attachmentRequests) {
    // 1. 첨부파일 저장
    List<UUID> attachmentIds = new ArrayList<>();
    if (attachmentRequests != null) {
        for (BinaryContentCreateRequest attachmentRequest : attachmentRequests) {
            BinaryContent attachment = new BinaryContent(
                attachmentRequest.fileName(),
                attachmentRequest.contentType(),
                attachmentRequest.data()
            );
            UUID attachmentId = binaryContentRepository.save(attachment).getId();
            attachmentIds.add(attachmentId);
        }
    }

    // 2. Message 엔티티 생성 및 저장
    Message message = new Message(
        request.content(),
        request.channelId(),
        request.authorId(),
        attachmentIds
    );
    Message savedMessage = messageRepository.save(message);

    // 3. MessageResponse 반환
    return toMessageResponse(savedMessage);
}
```

**첨부파일 처리 순서:**
1. 첨부파일을 먼저 BinaryContent로 저장
2. 저장된 파일의 ID 목록 수집
3. Message 엔티티에 ID 목록 저장

---

## 6. 데이터 흐름 예시

### 6.1 사용자 등록 전체 흐름

```
[Postman]
POST http://localhost:8080/users
Body: {
  "username": "alice",
  "email": "alice@example.com",
  "password": "pass123"
}

↓ HTTP 요청

[UserController]
createUser(UserCreateRequest)
  ↓ Service 호출

[BasicUserService]
create(UserCreateRequest, null)
  ├─ userRepository.existsByUsername("alice") → false ✅
  ├─ userRepository.existsByEmail("alice@example.com") → false ✅
  ├─ new User("alice", "alice@example.com", "pass123", null)
  ├─ userRepository.save(user)
  │   ↓
  │  [JCFUserRepository]
  │  data.put(user.getId(), user) → HashMap에 저장
  │   ↑
  ├─ new UserStatus(userId, Instant.now())
  ├─ userStatusRepository.save(userStatus)
  │   ↓
  │  [JCFUserStatusRepository]
  │  data.put(userStatus.getId(), userStatus)
  │   ↑
  └─ toUserResponse(user, true)
      ↓

[UserController]
ResponseEntity.status(201).body(userResponse)
  ↓ HTTP 응답

[Postman]
HTTP/1.1 201 Created
{
  "id": "uuid-generated",
  "username": "alice",
  "email": "alice@example.com",
  "profileId": null,
  "isOnline": true,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

### 6.2 채널 삭제 전체 흐름 (연쇄 삭제)

```
[Postman]
DELETE http://localhost:8080/channels/{channelId}

↓

[ChannelController]
deleteChannel(UUID id)
  ↓

[BasicChannelService]
delete(UUID id)
  ├─ channelRepository.findById(id) → Channel 확인
  │
  ├─ messageRepository.findAllByChannelId(id) → 메시지 목록 조회
  │   [메시지1, 메시지2, 메시지3]
  │
  ├─ for (Message message : messages) {
  │     for (UUID attachmentId : message.getAttachmentIds()) {
  │       ├─ binaryContentRepository.deleteById(attachmentId)
  │       │   ↓
  │       │  [JCFBinaryContentRepository]
  │       │  data.remove(attachmentId) → 첨부파일 삭제
  │       │   ↑
  │     }
  │   }
  │
  ├─ messageRepository.deleteAllByChannelId(id)
  │   ↓
  │  [JCFMessageRepository]
  │  data.values().removeIf(m -> m.getChannelId().equals(id))
  │   ↑
  │
  ├─ readStatusRepository.deleteAllByChannelId(id)
  │   ↓
  │  [JCFReadStatusRepository]
  │  data.values().removeIf(r -> r.getChannelId().equals(id))
  │   ↑
  │
  └─ channelRepository.deleteById(id)
      ↓
     [JCFChannelRepository]
     data.remove(id) → 채널 삭제
      ↑

[ChannelController]
ResponseEntity.noContent().build()
  ↓

[Postman]
HTTP/1.1 204 No Content
```

**삭제 순서가 중요한 이유:**
- 메시지가 첨부파일을 참조하므로 첨부파일을 먼저 삭제
- 채널이 메시지와 ReadStatus를 참조하므로 이들을 먼저 삭제
- 참조 무결성 유지

---

## 7. 초보자를 위한 핵심 개념

### 7.1 왜 계층을 나누는가?

**관심사의 분리 (Separation of Concerns)**

```
만약 계층을 나누지 않는다면?

UserController {
    public UserResponse createUser(UserCreateRequest request) {
        // HTTP 처리 + 비즈니스 로직 + 데이터 접근이 섞여있음
        if (data.containsKey(request.username())) { // 데이터 접근
            throw new Exception("중복!"); // 비즈니스 로직
        }
        User user = new User(...);
        data.put(user.getId(), user); // 데이터 접근
        return new UserResponse(...); // HTTP 응답
    }
}

문제점:
❌ 코드가 복잡하고 이해하기 어려움
❌ 테스트하기 어려움
❌ 데이터베이스 변경 시 Controller도 수정해야 함
❌ 재사용 불가능 (다른 Controller에서 같은 로직 필요 시 복사해야 함)
```

**계층을 나눈 경우:**

```
Controller: HTTP만 처리
  ↓
Service: 비즈니스 로직만 처리
  ↓
Repository: 데이터 접근만 처리

장점:
✅ 각 계층이 하나의 책임만 가짐 (Single Responsibility Principle)
✅ 테스트하기 쉬움 (Mock 객체 사용)
✅ 재사용 가능 (Service는 여러 Controller에서 사용)
✅ 유지보수 쉬움 (변경 시 해당 계층만 수정)
```

### 7.2 왜 DTO를 사용하는가?

**Entity를 직접 반환하면 안 되는 이유:**

```java
// ❌ 나쁜 예: Entity를 직접 반환
@RequestMapping(method = RequestMethod.GET, value = "/{id}")
public User getUser(@PathVariable UUID id) {
    return userRepository.findById(id).get();
}

// 응답:
{
  "id": "uuid",
  "username": "alice",
  "email": "alice@example.com",
  "password": "pass123",  // ❌ 비밀번호 노출!
  "createdAt": "...",
  "updatedAt": "..."
  // isOnline 같은 계산된 값은 없음
}
```

```java
// ✅ 좋은 예: DTO 사용
@RequestMapping(method = RequestMethod.GET, value = "/{id}")
public UserResponse getUser(@PathVariable UUID id) {
    return userService.find(id);
}

// 응답:
{
  "id": "uuid",
  "username": "alice",
  "email": "alice@example.com",
  // password 없음 ✅
  "isOnline": true,  // ✅ 계산된 값 추가
  "createdAt": "...",
  "updatedAt": "..."
}
```

**DTO의 장점:**
1. **보안**: 민감 정보 제외 (비밀번호)
2. **유연성**: 필요한 정보만 전송
3. **확장성**: 계산된 값 추가 가능
4. **안정성**: API 응답 구조를 독립적으로 관리

### 7.3 왜 인터페이스를 사용하는가?

```java
// UserService 인터페이스
public interface UserService {
    UserResponse create(UserCreateRequest request, ...);
    UserResponse find(UUID id);
    // ...
}

// 구현체 1: 기본 구현
@Service
public class BasicUserService implements UserService {
    // 비즈니스 로직 구현
}

// 나중에 구현체 2 추가 가능: 캐싱 추가
@Service
@Primary
public class CachedUserService implements UserService {
    private final BasicUserService basicUserService;
    private final Cache cache;

    public UserResponse find(UUID id) {
        if (cache.has(id)) {
            return cache.get(id);
        }
        UserResponse user = basicUserService.find(id);
        cache.put(id, user);
        return user;
    }
}
```

**장점:**
- Controller는 인터페이스에만 의존 → 구현체 교체 가능
- 테스트 시 Mock 구현체 사용 가능
- 여러 구현체를 만들어 기능 확장 가능

### 7.4 의존성 주입 (Dependency Injection)

**수동 방식 (DI 없이):**

```java
public class UserController {
    // ❌ 직접 객체 생성
    private UserService userService = new BasicUserService(
        new JCFUserRepository(),
        new JCFBinaryContentRepository(),
        new JCFUserStatusRepository()
    );
}

문제점:
❌ 결합도 높음 (특정 구현체에 의존)
❌ 테스트 어려움
❌ 설정 변경 시 코드 수정 필요
```

**Spring의 DI:**

```java
@RestController
@RequiredArgsConstructor  // Lombok: final 필드로 생성자 자동 생성
public class UserController {
    // ✅ Spring이 자동으로 주입
    private final UserService userService;
}

장점:
✅ Spring이 객체 생성 및 주입 자동 처리
✅ 인터페이스 타입으로 선언 → 구현체 교체 쉬움
✅ 테스트 시 Mock 주입 가능
```

### 7.5 메모리 저장소의 한계와 해결책

**현재 (JCF HashMap):**

```java
private final Map<UUID, User> data = new HashMap<>();
```

**문제점:**

1. **데이터 손실**
   - 서버 재시작 시 모든 데이터 삭제
   - 해결: 데이터베이스 사용 (MySQL, PostgreSQL)

2. **동시성 문제**
   - HashMap은 thread-safe 하지 않음
   - 여러 요청이 동시에 접근하면 데이터 손상 가능
   - 해결: `ConcurrentHashMap` 또는 데이터베이스 트랜잭션

3. **메모리 제한**
   - 데이터가 많아지면 OutOfMemoryError
   - 해결: 데이터베이스 + 페이징

**실무 해결책 (JPA):**

```java
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    // Spring Data JPA가 자동으로 구현 생성!
}

// application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/discodeit
    username: root
    password: password
  jpa:
    hibernate:
      ddl-auto: update
```

---

## 8. 실습 예제

### 8.1 API 테스트 (Postman)

#### 1단계: 사용자 등록

```
POST http://localhost:8080/users
Content-Type: application/json

{
  "username": "alice",
  "email": "alice@example.com",
  "password": "password123"
}

응답 (201 Created):
{
  "id": "generated-uuid",
  "username": "alice",
  "email": "alice@example.com",
  "profileId": null,
  "isOnline": true,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

#### 2단계: 로그인

```
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "username": "alice",
  "password": "password123"
}

응답 (200 OK):
{
  "id": "generated-uuid",
  "username": "alice",
  "email": "alice@example.com",
  "isOnline": true,
  ...
}
```

#### 3단계: 공개 채널 생성

```
POST http://localhost:8080/channels/public
Content-Type: application/json

{
  "name": "일반 대화",
  "description": "자유롭게 대화하는 공간"
}

응답 (201 Created):
{
  "id": "channel-uuid",
  "type": "PUBLIC",
  "name": "일반 대화",
  "description": "자유롭게 대화하는 공간",
  ...
}
```

#### 4단계: 메시지 전송

```
POST http://localhost:8080/messages
Content-Type: application/json

{
  "content": "안녕하세요!",
  "channelId": "channel-uuid",
  "authorId": "alice-uuid"
}

응답 (201 Created):
{
  "id": "message-uuid",
  "content": "안녕하세요!",
  "channelId": "channel-uuid",
  "authorId": "alice-uuid",
  "attachmentIds": [],
  ...
}
```

#### 5단계: 채널의 메시지 조회

```
GET http://localhost:8080/messages?channelId=channel-uuid

응답 (200 OK):
[
  {
    "id": "message-uuid",
    "content": "안녕하세요!",
    "channelId": "channel-uuid",
    "authorId": "alice-uuid",
    ...
  }
]
```

### 8.2 코드 수정 실습

#### 실습 1: 새로운 API 추가

**목표: 사용자 이름으로 사용자 검색 API 추가**

```java
// 1. UserController에 메서드 추가
@RequestMapping(method = RequestMethod.GET, value = "/search")
public ResponseEntity<UserResponse> getUserByUsername(
        @RequestParam String username
) {
    UserResponse user = userService.findByUsername(username);
    return ResponseEntity.ok(user);
}

// 2. UserService 인터페이스에 메서드 선언
public interface UserService {
    // ... 기존 메서드들
    UserResponse findByUsername(String username);
}

// 3. BasicUserService에 메서드 구현
@Override
public UserResponse findByUsername(String username) {
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new NoSuchElementException(
                "User not found with username: " + username
            ));

    boolean isOnline = getOnlineStatus(user.getId());
    return toUserResponse(user, isOnline);
}

// 4. 테스트
GET http://localhost:8080/users/search?username=alice
```

#### 실습 2: 비즈니스 로직 수정

**목표: 온라인 기준을 5분에서 10분으로 변경**

```java
// UserStatus.java
public boolean isOnline() {
    return Duration.between(lastActiveAt, Instant.now())
                   .toMinutes() < 10;  // 5 → 10으로 변경
}
```

#### 실습 3: DTO 필드 추가

**목표: UserResponse에 생성 후 경과 시간 추가**

```java
// UserResponse.java
public record UserResponse(
    UUID id,
    String username,
    String email,
    UUID profileId,
    boolean isOnline,
    Instant createdAt,
    Instant updatedAt,
    long createdDaysAgo  // 새 필드 추가
) {}

// BasicUserService.java
private UserResponse toUserResponse(User user, boolean isOnline) {
    long daysAgo = Duration.between(user.getCreatedAt(), Instant.now())
                           .toDays();

    return new UserResponse(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getProfileId(),
        isOnline,
        user.getCreatedAt(),
        user.getUpdatedAt(),
        daysAgo  // 계산된 값
    );
}
```

---

## 🎓 학습 체크리스트

### 이해했는지 확인해보세요:

- [ ] **아키텍처**: 계층형 구조의 각 계층 역할을 설명할 수 있나요?
- [ ] **Entity**: BaseEntity를 상속하는 이유를 아나요?
- [ ] **Repository**: JCF 구현체가 HashMap을 사용하는 이유를 아나요?
- [ ] **Service**: 비즈니스 로직이 왜 Service 계층에 있는지 아나요?
- [ ] **Controller**: `@RequestMapping`, `@RequestBody`, `@PathVariable`의 역할을 아나요?
- [ ] **DTO**: Entity를 직접 반환하지 않고 DTO를 사용하는 이유를 아나요?
- [ ] **DI**: Spring이 어떻게 의존성을 자동 주입하는지 아나요?
- [ ] **데이터 흐름**: HTTP 요청부터 응답까지의 전체 흐름을 설명할 수 있나요?
- [ ] **연쇄 삭제**: 채널 삭제 시 메시지와 첨부파일을 먼저 삭제하는 이유를 아나요?
- [ ] **PRIVATE 채널**: ReadStatus가 어떻게 접근 제어에 사용되는지 아나요?

---

## 📚 더 배우고 싶다면

### 다음 단계:

1. **JPA로 전환하기**
   - Spring Data JPA 학습
   - 데이터베이스 연동 (MySQL, PostgreSQL)
   - 트랜잭션 관리 (`@Transactional`)

2. **보안 추가하기**
   - Spring Security 적용
   - JWT 토큰 인증
   - 비밀번호 암호화 (BCrypt)

3. **테스트 작성하기**
   - JUnit 5
   - Mockito (Service 테스트)
   - MockMvc (Controller 테스트)

4. **실시간 기능 추가**
   - WebSocket으로 실시간 메시지
   - Server-Sent Events (SSE)

5. **고급 기능**
   - 파일 업로드 (Multipart)
   - 페이징 및 정렬
   - 검색 기능 (QueryDSL)
   - 캐싱 (Redis)

---

## 🎉 마무리

이 프로젝트는 **Spring Boot의 기본 개념들을 실제로 적용한 깔끔한 예제**입니다.

**핵심 포인트:**
- ✅ 계층형 아키텍처로 관심사 분리
- ✅ Repository 패턴으로 데이터 접근 추상화
- ✅ DTO로 API 응답 제어
- ✅ Spring DI로 객체 관리 자동화

**이 프로젝트를 이해했다면:**
- 실무 Spring Boot 프로젝트의 70% 이상 이해 가능!
- JPA, Security, WebSocket 등 추가 기술 학습 준비 완료!

**Happy Coding! 🚀**
