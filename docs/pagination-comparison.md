# 오프셋 페이지네이션 vs 커서 페이지네이션

## 오프셋 페이지네이션 (Offset Pagination)

### 개념
`LIMIT`과 `OFFSET`을 사용하여 특정 위치부터 N개의 데이터를 가져오는 방식.

```sql
SELECT * FROM messages ORDER BY created_at DESC LIMIT 50 OFFSET 100;
```

### 특징
- **직관적인 구현**: `page` 번호와 `size`만 있으면 원하는 페이지로 바로 이동 가능
- **임의 페이지 접근**: 3페이지, 10페이지 등 원하는 페이지로 바로 이동 가능
- **전체 개수 제공**: `totalElements`, `totalPages` 계산 가능

### 단점
- **성능 저하**: `OFFSET`이 클수록 DB가 앞의 데이터를 모두 읽고 버려야 하므로 느려짐 (OFFSET 1000이면 1000개 읽은 후 버림)
- **데이터 중복/누락**: 조회 중 새 데이터가 추가/삭제되면 다음 페이지에 이전 데이터가 다시 나오거나 빠질 수 있음
  - 예: 1페이지 조회 후 새 데이터가 삽입되면, 2페이지 조회 시 1페이지 마지막 항목이 다시 등장

### 사용 예시 (스프린트 미션 6-1)
```java
// 기존 구현
Slice<Message> findAllByChannel_Id(UUID channelId, Pageable pageable);

// 호출
Pageable pageable = PageRequest.of(0, 50, Sort.by(DESC, "createdAt"));
```

---

## 커서 페이지네이션 (Cursor Pagination)

### 개념
마지막으로 조회한 데이터의 특정 값(커서)을 기준으로 그 이후의 데이터를 가져오는 방식.

```sql
-- cursor = '2024-01-15T10:00:00Z' (이전 페이지의 마지막 createdAt)
SELECT * FROM messages
WHERE channel_id = ? AND created_at < ?  -- cursor 기준
ORDER BY created_at DESC
LIMIT 50;
```

### 특징
- **일관된 성능**: 어떤 페이지를 조회해도 항상 인덱스를 타는 동일한 성능 (WHERE 조건으로 인덱스 활용)
- **데이터 일관성**: 조회 중 새 데이터가 추가되어도 커서 이후의 데이터만 가져오므로 중복/누락 없음
- **무한 스크롤에 최적**: 소셜미디어, 채팅 등 끝없이 스크롤하는 UI에 적합

### 단점
- **임의 페이지 접근 불가**: "5페이지로 가기" 같은 기능 구현 불가
- **전체 개수 계산 어려움**: `totalPages` 계산에 별도 COUNT 쿼리 필요
- **정렬 기준 제약**: 커서로 사용하는 컬럼이 고유해야 하거나 복합 커서가 필요 (동일한 createdAt 값이 있으면 문제)

### 사용 예시 (스프린트 미션 6-2)
```java
// 커서 기반 쿼리
@Query("""
    SELECT m FROM Message m
    JOIN FETCH m.author a
    LEFT JOIN FETCH a.profile
    LEFT JOIN FETCH a.status
    WHERE m.channel.id = :channelId
      AND (:cursor IS NULL OR m.createdAt < :cursor)
    ORDER BY m.createdAt DESC
    """)
Slice<Message> findAllByChannelIdWithCursor(
    @Param("channelId") UUID channelId,
    @Param("cursor") Instant cursor,
    Pageable pageable);

// 응답에 nextCursor 포함
Instant nextCursor = slice.getContent().isEmpty() ? null
    : slice.getContent().get(slice.getContent().size() - 1).getCreatedAt();
```

### API 호출 흐름
```
1차 호출: GET /api/messages?channelId=xxx&size=50
  → 응답: { content: [...50개], nextCursor: "2024-01-10T09:00:00Z", hasNext: true }

2차 호출: GET /api/messages?channelId=xxx&cursor=2024-01-10T09:00:00Z&size=50
  → 응답: { content: [...50개], nextCursor: "2024-01-05T12:00:00Z", hasNext: true }

3차 호출 (마지막): GET /api/messages?channelId=xxx&cursor=2024-01-05T12:00:00Z&size=50
  → 응답: { content: [...30개], nextCursor: "2024-01-01T00:00:00Z", hasNext: false }
```

---

## 비교 요약

| 항목 | 오프셋 페이지네이션 | 커서 페이지네이션 |
|------|---------------------|-------------------|
| 구현 난이도 | 쉬움 | 보통 |
| 성능 (대용량) | 느림 (OFFSET 증가 시) | 빠름 (인덱스 활용) |
| 데이터 일관성 | 낮음 (중복/누락 가능) | 높음 |
| 임의 페이지 접근 | 가능 | 불가능 |
| 전체 개수 제공 | 쉬움 | 어려움 |
| 적합한 UI | 페이지 번호 UI | 무한 스크롤, 채팅 |

## 선택 기준

- **오프셋**: 게시판, 검색 결과 등 페이지 번호로 이동이 필요한 경우
- **커서**: 채팅, 피드, 타임라인 등 실시간으로 데이터가 추가되고 무한 스크롤이 필요한 경우 → **Discodeit 메시지 조회에 적합**
