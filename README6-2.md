# 스프린트 미션 6-2 심화 요구사항 구현 정리

> 초보자도 이해할 수 있도록, **왜 이렇게 코딩했는지**를 중심으로 설명합니다.

---

## 목차

1. [N+1 문제 해결](#1-n1-문제-해결)
2. [읽기 전용 트랜잭션과 OSIV 비활성화](#2-읽기-전용-트랜잭션과-osiv-비활성화)
3. [오프셋 vs 커서 페이지네이션 비교](#3-오프셋-vs-커서-페이지네이션-비교)
4. [커서 페이지네이션 구현](#4-커서-페이지네이션-구현)
5. [MapStruct 적용](#5-mapstruct-적용)
6. [API 명세서 v1.2 준수](#6-api-명세서-v12-준수)

---

## 1. N+1 문제 해결

### N+1 문제가 뭔가요?

데이터베이스에서 목록 1건을 조회한 뒤(1번 쿼리), 그 안에 있는 각 항목마다 추가 쿼리가 N번 실행되는 문제입니다.

```
예) 메시지 50개를 조회할 때:
  1번: SELECT * FROM messages WHERE channel_id = ? LIMIT 50
  2번: SELECT * FROM users WHERE id = ? (author)  ← 메시지마다 반복
  3번: SELECT * FROM users WHERE id = ? (author)
  ...
  51번: SELECT * FROM users WHERE id = ? (author)
총 51번 쿼리 → 성능 저하!
```

### 어떻게 해결했나요?

#### 해결 방법 1: JOIN FETCH (한 번의 쿼리로 함께 조회)

```java
// MessageRepository.java
@Query("""
    SELECT m FROM Message m
    JOIN FETCH m.author a          ← 작성자를 한 번에 같이 가져옴
    LEFT JOIN FETCH a.profile      ← 작성자 프로필 이미지도 함께
    LEFT JOIN FETCH a.status       ← 작성자 온라인 상태도 함께
    WHERE m.channel.id = :channelId
    ORDER BY m.createdAt DESC
    """)
Slice<Message> findAllByChannelId(...);
```

**왜 JOIN FETCH를 썼나요?**
- JPA의 기본 동작은 연관 데이터를 `LAZY`(나중에 필요할 때)로 가져옵니다
- 메시지 목록을 조회할 때 작성자 정보가 반드시 필요하므로, `JOIN FETCH`로 한 번에 가져오면 추가 쿼리가 사라집니다

#### 해결 방법 2: 배치 조회 (여러 건을 한 번에)

```java
// Message.java (엔티티)
@BatchSize(size = 100)         ← 100개씩 묶어서 한 번에 조회
@ManyToMany(...)
private List<BinaryContent> attachments = new ArrayList<>();
```

**왜 @BatchSize를 썼나요?**
- 첨부파일은 `@ManyToMany`(다대다 관계)라서 JOIN FETCH를 쓰면 페이지네이션이 메모리에서 처리되는 문제가 생깁니다
- `@BatchSize(100)`을 쓰면 "첨부파일 목록은 100개씩 묶어서 쿼리 1번으로 가져와라"는 의미입니다
- 메시지 50개의 첨부파일을 가져올 때 → 50번 쿼리 대신 1번으로 해결

#### 해결 방법 3: 채널 목록 조회 최적화

채널 목록을 조회할 때 문제가 2가지 있었습니다:
1. 각 채널의 마지막 메시지 시간을 채널마다 별도로 조회
2. Private 채널 참여자 정보를 반복 조회

```java
// ChannelRepository.java
@Query("""
    SELECT DISTINCT c FROM Channel c
    LEFT JOIN FETCH c.readStatuses rs   ← 읽음 상태를 한 번에
    LEFT JOIN FETCH rs.user u           ← 참여자 정보를 한 번에
    LEFT JOIN FETCH u.profile           ← 참여자 프로필도 한 번에
    LEFT JOIN FETCH u.status            ← 참여자 온라인 상태도 한 번에
    WHERE c.type = :type OR EXISTS (...)
    """)
List<Channel> findAllByUserWithDetails(...);
```

```java
// MessageRepository.java
// 여러 채널의 마지막 메시지 시간을 한 번의 쿼리로 조회
@Query("SELECT m.channel.id, MAX(m.createdAt) FROM Message m
        WHERE m.channel.id IN :channelIds
        GROUP BY m.channel.id")
List<Object[]> findLastCreatedAtByChannelIds(...);
```

```java
// BasicChannelService.java - 서비스에서 사용하는 모습
@Transactional(readOnly = true)
public List<ChannelDto> findAllByUserId(UUID userId) {
    // 1번: 채널 + 참여자 정보 한 번에 조회
    List<Channel> channels = channelRepository.findAllByUserWithDetails(PUBLIC, userId);
    List<UUID> channelIds = channels.stream().map(Channel::getId).toList();

    // 2번: 마지막 메시지 시간 한 번에 조회 (채널 수와 무관하게 항상 1번)
    Map<UUID, Instant> lastMessageAtMap = messageRepository
        .findLastCreatedAtByChannelIds(channelIds)
        .stream()
        .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Instant) row[1]));

    // 이미 다 로딩되어 있으니 추가 쿼리 없이 DTO 변환
    return channels.stream()
        .map(channel -> channelMapper.toDto(channel, participants, lastMessageAt))
        .toList();
}
```

**결과:**
| 상황 | 개선 전 | 개선 후 |
|------|---------|---------|
| 메시지 50개 조회 | 51번 이상 쿼리 | 2~3번 쿼리 |
| 채널 10개 조회 | 30번 이상 쿼리 | 2번 쿼리 |

---

## 2. 읽기 전용 트랜잭션과 OSIV 비활성화

### OSIV가 뭔가요?

**OSIV(Open Session In View)**: JPA 세션(DB 연결)을 HTTP 요청이 끝날 때까지 열어두는 기능입니다.

```
OSIV 활성화 시 (기본값):
  HTTP 요청 시작 → [컨트롤러 → 서비스 → DB 조회] → [컨트롤러 → 뷰 렌더링] → HTTP 응답
                    ←─────────────── 세션 열려있음 ────────────────→

  문제점: 뷰(또는 DTO 변환) 단계에서 DB 쿼리가 실행될 수 있어
          개발자가 모르는 사이에 쿼리가 발생!
```

### 왜 OSIV를 비활성화했나요?

```yaml
# application.yaml
spring:
  jpa:
    open-in-view: false  # OSIV 비활성화
```

- 프로덕션 환경에서는 성능상 OSIV를 끄는 것이 권장됩니다
- OSIV를 끄면 트랜잭션 밖에서 `LAZY` 로딩 시도 시 `LazyInitializationException` 에러 발생
- 이 에러가 "어디서 쿼리가 실행되는지 모르는" 버그를 방지해줍니다

### 어떻게 대응했나요?

**`@Transactional(readOnly = true)` 사용:**

```java
// BasicMessageService.java
@Override
@Transactional(readOnly = true)          // 읽기 전용 트랜잭션
public PageResponse<MessageDto> findAllByChannelId(...) {
    // 이 블록 안에서만 DB 세션 유지
    // JOIN FETCH로 필요한 데이터를 모두 미리 로딩했으므로 안전
    Slice<Message> slice = messageRepository.findAllByChannelId(channelId, pageable);
    Slice<MessageDto> dtoSlice = slice.map(messageMapper::toDto);  // 트랜잭션 안에서 변환
    return pageResponseMapper.fromSlice(dtoSlice, nextCursor);
}   // 여기서 트랜잭션 종료 → 세션 닫힘
```

**`readOnly = true`의 장점:**

| 항목 | 설명 |
|------|------|
| 성능 향상 | Hibernate가 변경 감지(dirty checking)를 생략 → 더 빠름 |
| 명확한 의도 | "이 메서드는 데이터를 변경하지 않는다"는 것을 코드로 표현 |
| DB 최적화 | 일부 DB는 읽기 전용 트랜잭션을 최적화하여 처리 |

**규칙 요약:**
- 데이터 조회 메서드 → `@Transactional(readOnly = true)`
- 데이터 변경(저장, 수정, 삭제) 메서드 → `@Transactional`

---

## 3. 오프셋 vs 커서 페이지네이션 비교

> 상세 비교 문서: `docs/pagination-comparison.md` 참고

### 오프셋 페이지네이션

```sql
-- 3페이지 (1페이지당 10개씩)
SELECT * FROM messages ORDER BY created_at DESC LIMIT 10 OFFSET 20;
```

```
장점: "3페이지로 이동" 같은 임의 페이지 접근 가능
단점:
  - OFFSET이 클수록 DB가 앞 데이터를 읽고 버려야 해서 느려짐
  - 조회 중 새 데이터가 추가되면 중복/누락 발생 가능
```

### 커서 페이지네이션

```sql
-- 이전 페이지의 마지막 메시지 시간(cursor)보다 이전 데이터 조회
SELECT * FROM messages
WHERE created_at < '2024-01-15T10:00:00Z'  ← cursor 기준
ORDER BY created_at DESC
LIMIT 50;
```

```
장점:
  - 항상 인덱스를 활용 → 데이터가 많아져도 성능 일정
  - 실시간으로 새 메시지가 추가돼도 중복/누락 없음
단점:
  - "n페이지로 이동" 불가
  - 전체 페이지 수 계산 어려움
```

**왜 채팅 메시지에는 커서 페이지네이션이 적합한가?**
- 채팅은 실시간으로 메시지가 계속 추가됩니다
- 사용자는 "위로 스크롤 → 이전 메시지 더 보기" 패턴을 사용합니다
- "5페이지로 이동" 같은 기능이 필요 없습니다

---

## 4. 커서 페이지네이션 구현

### PageResponse 구조 변경

```java
// 변경 전 (오프셋 기반)
public record PageResponse<T>(
    List<T> content,
    int number,       // 현재 페이지 번호 → 제거
    int size,
    boolean hasNext,
    Long totalElements
) {}

// 변경 후 (커서 기반)
public record PageResponse<T>(
    List<T> content,
    Object nextCursor,  // 다음 페이지 조회용 커서 값 → 추가
    int size,
    boolean hasNext,
    Long totalElements
) {}
```

### 동작 흐름

```
1. 첫 번째 요청:
   GET /api/messages?channelId=xxx&size=50

   응답:
   {
     "content": [ {id: "msg50", createdAt: "2024-01-15..."}, ... ],
     "nextCursor": "2024-01-01T00:00:00Z",  ← 마지막 메시지의 createdAt
     "hasNext": true
   }

2. 다음 페이지 요청 (스크롤을 위로 올렸을 때):
   GET /api/messages?channelId=xxx&cursor=2024-01-01T00:00:00Z&size=50

   응답:
   {
     "content": [ {id: "msg49", ...}, ... ],
     "nextCursor": "2023-12-15T00:00:00Z",
     "hasNext": false  ← 더 이상 없음
   }
```

### 백엔드 구현

```java
// MessageRepository.java - 두 개의 쿼리로 분리한 이유
// cursor가 null이면 PostgreSQL이 파라미터 타입을 추론 못하는 오류 발생
// → cursor 유무에 따라 메서드를 분리

// cursor 없을 때 (첫 로딩, 폴링)
@Query("SELECT m FROM Message m JOIN FETCH m.author a ... WHERE m.channel.id = :channelId ORDER BY m.createdAt DESC")
Slice<Message> findAllByChannelId(@Param("channelId") UUID channelId, Pageable pageable);

// cursor 있을 때 (이전 메시지 더 보기)
@Query("SELECT m FROM Message m JOIN FETCH m.author a ... WHERE m.channel.id = :channelId AND m.createdAt < :cursor ORDER BY m.createdAt DESC")
Slice<Message> findAllByChannelIdBeforeCursor(@Param("channelId") UUID channelId, @Param("cursor") Instant cursor, Pageable pageable);
```

```java
// BasicMessageService.java - 서비스에서 분기 처리
@Transactional(readOnly = true)
public PageResponse<MessageDto> findAllByChannelId(UUID channelId, Instant cursor, Pageable pageable) {
    PageRequest sizeOnly = PageRequest.of(0, pageable.getPageSize());

    // cursor 유무로 분기
    Slice<Message> slice = (cursor == null)
        ? messageRepository.findAllByChannelId(channelId, sizeOnly)
        : messageRepository.findAllByChannelIdBeforeCursor(channelId, cursor, sizeOnly);

    // 응답의 nextCursor = 현재 페이지 마지막 메시지의 createdAt
    Instant nextCursor = slice.getContent().isEmpty() ? null
        : slice.getContent().get(slice.getContent().size() - 1).getCreatedAt();

    return pageResponseMapper.fromSlice(slice.map(messageMapper::toDto), nextCursor);
}
```

---

## 5. MapStruct 적용

### MapStruct가 뭔가요?

Entity(DB 데이터) → DTO(클라이언트에 보낼 데이터)로 변환하는 **보일러플레이트 코드를 자동 생성**해주는 라이브러리입니다.

### 왜 쓰나요?

```java
// 기존 수동 변환 코드 (반복적이고 실수하기 쉬움)
public BinaryContentDto toDto(BinaryContent bc) {
    if (bc == null) return null;
    return new BinaryContentDto(
        bc.getId(),
        bc.getFileName(),
        bc.getSize(),
        bc.getContentType()
    );
}

// MapStruct 적용 후 (인터페이스만 선언하면 끝)
@Mapper(componentModel = "spring")
public interface BinaryContentMapper {
    BinaryContentDto toDto(BinaryContent binaryContent);
    // ↑ 빌드 시 구현 코드 자동 생성
}
```

### 각 Mapper별 설명

#### BinaryContentMapper - 가장 단순한 경우

```java
@Mapper(componentModel = "spring")
public interface BinaryContentMapper {
    BinaryContentDto toDto(BinaryContent binaryContent);
}
// 필드명이 같으면 MapStruct가 알아서 매핑: id→id, fileName→fileName, size→size
```

#### UserStatusMapper - 중첩 필드 접근

```java
@Mapper(componentModel = "spring")
public interface UserStatusMapper {
    @Mapping(target = "userId", source = "user.id")  // userStatus.user.id → userId
    UserStatusDto toDto(UserStatus userStatus);
}
```

#### ReadStatusMapper - 두 개의 중첩 필드

```java
@Mapper(componentModel = "spring")
public interface ReadStatusMapper {
    @Mapping(target = "userId", source = "user.id")      // user.id → userId
    @Mapping(target = "channelId", source = "channel.id") // channel.id → channelId
    ReadStatusDto toDto(ReadStatus readStatus);
}
```

#### UserMapper - Java 표현식 활용

```java
@Mapper(componentModel = "spring", uses = {BinaryContentMapper.class})
public interface UserMapper {
    @Mapping(
        target = "online",
        // UserStatus가 null일 수도 있으므로 null 체크 후 isOnline() 호출
        expression = "java(user.getStatus() != null ? user.getStatus().isOnline() : null)"
    )
    UserDto toDto(User user);
    // profile 필드는 uses에 등록된 BinaryContentMapper가 자동으로 변환
}
```

#### MessageMapper - 다른 Mapper를 조합

```java
@Mapper(componentModel = "spring", uses = {BinaryContentMapper.class, UserMapper.class})
public interface MessageMapper {
    @Mapping(target = "channelId", source = "channel.id")  // message.channel.id → channelId
    MessageDto toDto(Message message);
    // author: UserMapper가 자동으로 변환
    // attachments: BinaryContentMapper가 List 전체를 자동으로 변환
}
```

#### ChannelMapper - MapStruct 대신 수동 유지한 이유

```java
// ChannelMapper는 @Component로 직접 구현 (MapStruct 미적용)
@Component
public class ChannelMapper {
    public ChannelDto toDto(Channel channel, List<UserDto> participants, Instant lastMessageAt) {
        return new ChannelDto(channel.getId(), channel.getType(), ...);
    }
}
```

**왜 ChannelMapper는 MapStruct를 쓰지 않았나요?**
- `ChannelDto`의 `participants`와 `lastMessageAt`은 Channel 엔티티에 없는 필드입니다
- 서비스 레이어에서 DB를 조회해서 별도로 구성한 뒤 매퍼에 넘겨줍니다
- 이런 경우 MapStruct의 자동 매핑이 맞지 않으므로 수동으로 구현했습니다

### build.gradle 설정 주의사항

```groovy
// Lombok + MapStruct를 같이 쓸 때 선언 순서가 중요!
annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'  // 바인딩 먼저
annotationProcessor 'org.projectlombok:lombok'                           // 그 다음 Lombok
annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.5.Final'     // 마지막에 MapStruct
```

**왜 순서가 중요한가요?**
- MapStruct는 Lombok이 생성한 getter/setter를 기반으로 코드를 생성합니다
- Lombok이 먼저 실행되어야 MapStruct가 해당 getter/setter를 찾을 수 있습니다
- `lombok-mapstruct-binding`은 이 순서를 보장해주는 연결 라이브러리입니다

---

## 6. API 명세서 v1.2 준수

### 변경 사항 요약

#### PageResponse 구조 변경

| 필드 | v1.1 | v1.2 |
|------|------|------|
| `content` | ✅ 동일 | ✅ 동일 |
| `number` | ✅ 있음 | ❌ 제거 |
| `nextCursor` | ❌ 없음 | ✅ 추가 |
| `size` | ✅ 동일 | ✅ 동일 |
| `hasNext` | ✅ 동일 | ✅ 동일 |
| `totalElements` | ✅ 동일 | ✅ 동일 |

#### 메시지 조회 API 변경

```
GET /api/messages

파라미터 추가:
  cursor (optional): 페이징 커서 (이전 응답의 nextCursor 값)

예시:
  첫 조회: GET /api/messages?channelId=xxx&size=50
  더 보기: GET /api/messages?channelId=xxx&cursor=2024-01-01T00:00:00Z&size=50
```

### GlobalExceptionHandler 개선

```java
// 기존: e.printStackTrace() → 60줄짜리 스택트레이스 출력
// 개선: 적절한 로그 레벨로 교체

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 Not Found → WARN 레벨 (정상적인 "없음" 상황)
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleException(NoSuchElementException e) {
        log.warn("Not found: {}", e.getMessage());  // 한 줄만 출력
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    // 500 Internal Error → ERROR 레벨 (진짜 문제, 스택트레이스 포함)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        log.error("Unhandled exception", e);        // 전체 스택트레이스
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }
}
```

---

## 전체 흐름 요약

```
클라이언트 요청
    ↓
Controller (HTTP 매핑, Swagger 문서)
    ↓
Service @Transactional(readOnly = true)
    ↓                    ↓
Repository           Repository
(JOIN FETCH로      (배치 조회로
 N+1 해결)          N+1 해결)
    ↓
Mapper (MapStruct로 Entity → DTO 자동 변환)
    ↓
PageResponse (nextCursor 포함 커서 기반 응답)
    ↓
클라이언트 응답
```

## 핵심 개념 한 줄 요약

| 개념 | 한 줄 요약 |
|------|-----------|
| **N+1 문제** | 목록 1번 조회 후 각 항목마다 추가 쿼리가 발생하는 성능 문제 |
| **JOIN FETCH** | JPA에서 연관 데이터를 한 번의 쿼리로 함께 가져오는 방법 |
| **@BatchSize** | 여러 건의 컬렉션을 N번이 아닌 묶음으로 조회하는 방법 |
| **OSIV 비활성화** | DB 세션을 트랜잭션 범위로만 제한하는 설정 |
| **readOnly = true** | 조회 전용 트랜잭션 → 변경 감지 생략으로 성능 향상 |
| **오프셋 페이지네이션** | `LIMIT/OFFSET`으로 페이지 번호 기반 조회 |
| **커서 페이지네이션** | 마지막으로 본 데이터를 기준으로 그 이전/이후 조회 |
| **MapStruct** | Entity↔DTO 변환 코드를 빌드 시 자동 생성해주는 라이브러리 |
