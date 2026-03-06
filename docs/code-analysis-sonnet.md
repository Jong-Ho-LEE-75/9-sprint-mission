# API Spec v1.2 기준 불필요 코드 분석 보고서 (Sonnet)

## 1. API 엔드포인트 구현 현황

| 엔드포인트 | 스펙 | 구현 | 상태 |
|---|---|---|---|
| POST /api/auth/login | O | O | 정상 |
| GET /api/users | O | O | 정상 |
| POST /api/users | O | O | 정상 |
| PATCH /api/users/{userId} | O | O | 정상 |
| DELETE /api/users/{userId} | O | O | 정상 |
| PATCH /api/users/{userId}/userStatus | O | O | 정상 |
| GET /api/users/{userId}/userStatus | **없음** | O | 불필요 |
| GET /api/channels | O | O | 정상 |
| POST /api/channels/public | O | O | 정상 |
| POST /api/channels/private | O | O | 정상 |
| PATCH /api/channels/{channelId} | O | O | 정상 |
| DELETE /api/channels/{channelId} | O | O | 정상 |
| GET /api/messages | O | O | 정상 |
| POST /api/messages | O | O | 정상 |
| PATCH /api/messages/{messageId} | O | O | 정상 |
| DELETE /api/messages/{messageId} | O | O | 정상 |
| GET /api/readStatuses | O | O | 정상 |
| POST /api/readStatuses | O | O | 정상 |
| PATCH /api/readStatuses/{readStatusId} | O | O | 정상 |
| GET /api/binaryContents | O | O | 정상 |
| GET /api/binaryContents/{id} | O | O | 정상 |
| GET /api/binaryContents/{id}/download | O | O | 정상 |

---

## 2. 치명적 버그: PageResponse 필드 불일치

### 문제
API spec의 `PageResponse` 스키마:
```json
{ "content": [...], "number": 0, "size": 50, "hasNext": true, "totalElements": null }
```

현재 구현:
```java
public record PageResponse<T>(List<T> content, Object nextCursor, int size, boolean hasNext, Long totalElements)
```

프론트엔드 `messageStore.ts`:
```js
currentPage: response.number  // nextCursor 필드를 받아 undefined 반환
```

### 영향
- `pagination.currentPage`가 `undefined`
- 무한 스크롤 "더 불러오기"가 동작하지 않음 (`undefined + 1 = NaN`)

### 수정 내용
- `PageResponse.nextCursor` -> `number` (int)
- `PageResponseMapper.fromSlice()` - cursor 제거, `slice.getNumber()` 사용
- `MessageService`, `BasicMessageService`, `MessageController` - cursor 파라미터 제거

---

## 3. 불필요한 코드 목록 (삭제 대상)

### 3-1. API spec에 없는 엔드포인트
| 파일 | 내용 |
|---|---|
| `UserController.getUserStatusByUserId()` | GET /api/users/{userId}/userStatus (spec에 없음) |
| `UserApi.getUserStatusByUserId()` | 위 컨트롤러 메서드의 Swagger 인터페이스 |

### 3-2. API endpoint가 없는 서비스 메서드
| 파일 | 메서드 | 이유 |
|---|---|---|
| `MessageService.find(UUID)` | 인터페이스 | 대응하는 API endpoint 없음, 컨트롤러 미사용 |
| `BasicMessageService.find(UUID)` | 구현체 | 위와 동일 |
| `ChannelService.find(UUID)` | 인터페이스 | 대응하는 API endpoint 없음, 컨트롤러 미사용 |
| `BasicChannelService.find(UUID)` | 구현체 | 위와 동일 |

### 3-3. 사용되지 않는 DTO
| 파일 | 이유 |
|---|---|
| `UserStatusCreateRequest.java` | UserStatus는 User 생성 시 자동 생성, 별도 create endpoint/service 없음 |

### 3-4. 사용되지 않는 Repository 메서드
| 파일 | 메서드 | 이유 |
|---|---|---|
| `MessageRepository.findAllByChannelIdBeforeCursor()` | cursor 기반 페이지네이션 제거 후 미사용 |
| `MessageRepository.deleteAllByChannel_Id()` | DB CASCADE로 처리됨, 호출 없음 |
| `ReadStatusRepository.deleteAllByChannel_Id()` | DB CASCADE로 처리됨, 호출 없음 |

### 3-5. cursor 관련 코드
| 파일 | 내용 |
|---|---|
| `MessageService.findAllByChannelId(cursor 파라미터)` | API spec에 cursor 파라미터 없음 |
| `BasicMessageService.findAllByChannelId()` 내 cursor 분기 | 위와 동일 |
| `MessageController.findAllByChannelId()` 의 cursor @RequestParam | 위와 동일 |
